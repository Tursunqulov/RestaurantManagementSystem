package com.restaurant.service;

import com.restaurant.dao.NotificationDAO;
import com.restaurant.dao.ReservationDAO;
import com.restaurant.model.Notification;
import com.restaurant.model.Reservation;
import com.restaurant.model.enums.NotificationType;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Background <b>daemon thread</b> that periodically scans for upcoming
 * reservations and generates notification records.
 *
 * <h3>Multithreading Concepts Demonstrated:</h3>
 * <ul>
 *   <li><b>Daemon thread</b> — runs in the background and terminates
 *       automatically when the JVM exits (no dangling threads).</li>
 *   <li><b>Thread safety</b> — uses {@link CopyOnWriteArrayList} for
 *       the listener list (safe concurrent iteration + modification).</li>
 *   <li><b>volatile flag</b> — the {@code running} flag is {@code volatile}
 *       to ensure visibility across threads without synchronization.</li>
 *   <li><b>Runnable implementation</b> — the core loop is a {@link Runnable},
 *       keeping threading mechanics separate from business logic.</li>
 * </ul>
 *
 * <h3>SOLID — Single Responsibility:</h3>
 * <p>This service does exactly one thing: detect approaching reservations
 * and create notifications. It doesn't send emails or update the UI —
 * that responsibility belongs to registered listeners.</p>
 */
public class ReservationNotificationService {

    /** How often the daemon checks for upcoming reservations (in milliseconds). */
    private static final long CHECK_INTERVAL_MS = 60_000; // 1 minute

    /** How far ahead to look for upcoming reservations (in minutes). */
    private static final int LOOKAHEAD_MINUTES = 30;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ReservationDAO reservationDAO;
    private final NotificationDAO notificationDAO;

    private Thread daemonThread;
    private volatile boolean running = false;

    /**
     * Listeners notified on the <em>calling thread</em> when a new
     * notification is generated. The JavaFX controller can register
     * a listener and use {@code Platform.runLater()} to update the UI.
     */
    private final CopyOnWriteArrayList<Consumer<Notification>> listeners =
            new CopyOnWriteArrayList<>();

    // ── Constructor ─────────────────────────────────────────

    public ReservationNotificationService() {
        this.reservationDAO = new ReservationDAO();
        this.notificationDAO = new NotificationDAO();
    }

    // ── Lifecycle ───────────────────────────────────────────

    /**
     * Starts the background daemon thread.
     * Safe to call multiple times — only one thread will run.
     */
    public synchronized void start() {
        if (running) return;
        running = true;
        daemonThread = new Thread(this::monitorLoop, "ReservationNotifier");
        daemonThread.setDaemon(true);   // JVM will not wait for this thread
        daemonThread.start();
        System.out.println("[NotificationService] Daemon thread started.");
    }

    /**
     * Stops the daemon thread gracefully.
     */
    public synchronized void stop() {
        running = false;
        if (daemonThread != null) {
            daemonThread.interrupt();
            System.out.println("[NotificationService] Daemon thread stopped.");
        }
    }

    public boolean isRunning() {
        return running;
    }

    // ── Listener management ─────────────────────────────────

    public void addListener(Consumer<Notification> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<Notification> listener) {
        listeners.remove(listener);
    }

    // ── Core loop (runs on daemon thread) ───────────────────

    /**
     * The main monitoring loop. Runs until {@link #stop()} is called
     * or the JVM exits (daemon thread).
     */
    private void monitorLoop() {
        while (running) {
            try {
                checkUpcomingReservations();
                Thread.sleep(CHECK_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Log and continue — daemon should be resilient
                System.err.println("[NotificationService] Error: " + e.getMessage());
                try {
                    Thread.sleep(CHECK_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Scans for confirmed reservations within the next {@value #LOOKAHEAD_MINUTES}
     * minutes and creates a notification for each one (if not already notified).
     */
    private void checkUpcomingReservations() throws SQLException {
        LocalDateTime now  = LocalDateTime.now();
        LocalDateTime soon = now.plusMinutes(LOOKAHEAD_MINUTES);

        List<Reservation> upcoming = reservationDAO.findUpcoming(now, soon);

        for (Reservation res : upcoming) {
            // Skip if already notified
            if (notificationDAO.existsForReservation(res.getReservationId())) {
                continue;
            }

            String message = String.format(
                    "Reminder: Reservation #%d for %d guest(s) at Table %d is at %s.",
                    res.getReservationId(),
                    res.getPeopleCount(),
                    res.getTableId(),
                    res.getTimeOfReservation().format(FMT)
            );

            Notification notification = new Notification();
            notification.setReservationId(res.getReservationId());
            notification.setCustomerId(res.getCustomerId());
            notification.setContent(message);
            notification.setNotificationType(NotificationType.SYSTEM);
            notification.setSent(true); // Mark as "sent" (displayed to staff)

            notificationDAO.insert(notification);

            System.out.println("[NotificationService] " + message);

            // Notify registered listeners (e.g., JavaFX dashboard)
            for (Consumer<Notification> listener : listeners) {
                try {
                    listener.accept(notification);
                } catch (Exception e) {
                    System.err.println("[NotificationService] Listener error: " + e.getMessage());
                }
            }
        }
    }
}
