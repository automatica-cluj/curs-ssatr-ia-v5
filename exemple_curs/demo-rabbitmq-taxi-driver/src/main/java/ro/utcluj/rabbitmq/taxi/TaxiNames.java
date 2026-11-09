package ro.utcluj.rabbitmq.taxi;

/**
 * The RabbitMQ broker is shared by all students, so exchange and queue names get a per-student prefix.
 * The prefix is the OS user name; override it with -Dstudent=name (needed if students share an OS account).
 * All apps which must talk to each other have to run with the same prefix.
 */
final class TaxiNames {

    private static final String PREFIX = System.getProperty("student", System.getProperty("user.name")) + ".";

    static final String EXCHANGE = PREFIX + "booking-requests";
    static final String DRIVER_QUEUE = PREFIX + "driver-";
    static final String CLIENT_QUEUE = PREFIX + "orderconfirmation_";

    private TaxiNames() {
    }
}
