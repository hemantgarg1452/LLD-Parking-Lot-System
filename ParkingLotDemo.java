// PARKING LOT - Complete LLD Solution

// Step 1: Requirements
// - Multi-floor parking lot
// - Different vehicle types (car, bike, truck)
// - Different spot sizes (small, medium, large)
// - Entry/exit points
// - Ticket-based system
// - Hourly rate billing
// - Find nearest available spot

// Step 2: Core Entities
// ParkingLot, Floor, ParkingSpot, Vehicle, Ticket, 
// EntryPanel, ExitPanel, Payment

// Step 3 & 5: Implementation

// ──── Enums ────
public enum VehicleType {
    BIKE, CAR, TRUCK
}

public enum SpotSize {
    SMALL, MEDIUM, LARGE
}

public enum SpotStatus {
    AVAILABLE, OCCUPIED, MAINTENANCE
}

public enum TicketStatus {
    ACTIVE, PAID, LOST
}

// ──── Vehicle ────
public abstract class Vehicle {
    private String licensePlate;
    private VehicleType type;

    public Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }

    public abstract SpotSize getRequiredSpotSize();

    // getters
}

public class Car extends Vehicle {
    public Car(String licensePlate) {
        super(licensePlate, VehicleType.CAR);
    }

    @Override
    public SpotSize getRequiredSpotSize() {
        return SpotSize.MEDIUM;
    }
}

public class Bike extends Vehicle {
    public Bike(String licensePlate) {
        super(licensePlate, VehicleType.BIKE);
    }

    @Override
    public SpotSize getRequiredSpotSize() {
        return SpotSize.SMALL;
    }
}

public class Truck extends Vehicle {
    public Truck(String licensePlate) {
        super(licensePlate, VehicleType.TRUCK);
    }

    @Override
    public SpotSize getRequiredSpotSize() {
        return SpotSize.LARGE;
    }
}

// ──── Parking Spot ────
public class ParkingSpot {
    private final String spotId;
    private final SpotSize size;
    private final int floorNumber;
    private SpotStatus status;
    private Vehicle parkedVehicle;

    public ParkingSpot(String spotId, SpotSize size, int floorNumber) {
        this.spotId = spotId;
        this.size = size;
        this.floorNumber = floorNumber;
        this.status = SpotStatus.AVAILABLE;
    }

    public synchronized boolean park(Vehicle vehicle) {
        if (status != SpotStatus.AVAILABLE)
            return false;
        if (!canFitVehicle(vehicle))
            return false;

        this.parkedVehicle = vehicle;
        this.status = SpotStatus.OCCUPIED;
        return true;
    }

    public synchronized Vehicle unpark() {
        Vehicle vehicle = this.parkedVehicle;
        this.parkedVehicle = null;
        this.status = SpotStatus.AVAILABLE;
        return vehicle;
    }

    public boolean canFitVehicle(Vehicle vehicle) {
        return this.size.ordinal() >= vehicle.getRequiredSpotSize().ordinal();
    }

    public boolean isAvailable() {
        return status == SpotStatus.AVAILABLE;
    }

    // getters
}

// ──── Parking Floor ────
public class ParkingFloor {
    private final int floorNumber;
    private final Map<SpotSize, List<ParkingSpot>> spotsBySize;

    public ParkingFloor(int floorNumber) {
        this.floorNumber = floorNumber;
        this.spotsBySize = new EnumMap<>(SpotSize.class);
        for (SpotSize size : SpotSize.values()) {
            spotsBySize.put(size, new ArrayList<>());
        }
    }

    public void addSpot(ParkingSpot spot) {
        spotsBySize.get(spot.getSize()).add(spot);
    }

    public Optional<ParkingSpot> findAvailableSpot(SpotSize requiredSize) {
        // First try exact size, then try larger sizes
        for (SpotSize size : SpotSize.values()) {
            if (size.ordinal() >= requiredSize.ordinal()) {
                Optional<ParkingSpot> spot = spotsBySize.get(size).stream()
                        .filter(ParkingSpot::isAvailable)
                        .findFirst();
                if (spot.isPresent())
                    return spot;
            }
        }
        return Optional.empty();
    }

    public long getAvailableSpotCount(SpotSize size) {
        return spotsBySize.get(size).stream()
                .filter(ParkingSpot::isAvailable)
                .count();
    }
}

// ──── Ticket ────
public class ParkingTicket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private TicketStatus status;
    private double amountPaid;

    public ParkingTicket(Vehicle vehicle, ParkingSpot spot) {
        this.ticketId = generateTicketId();
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTime = LocalDateTime.now();
        this.status = TicketStatus.ACTIVE;
    }

    private String generateTicketId() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // getters and setters
}

// ──── Pricing Strategy (Strategy Pattern) ────
public interface PricingStrategy {
    double calculatePrice(ParkingTicket ticket);
}

public class HourlyPricingStrategy implements PricingStrategy {
    private Map<VehicleType, Double> hourlyRates;

    public HourlyPricingStrategy() {
        hourlyRates = new EnumMap<>(VehicleType.class);
        hourlyRates.put(VehicleType.BIKE, 10.0);
        hourlyRates.put(VehicleType.CAR, 20.0);
        hourlyRates.put(VehicleType.TRUCK, 30.0);
    }

    @Override
    public double calculatePrice(ParkingTicket ticket) {
        long hours = ChronoUnit.HOURS.between(
                ticket.getEntryTime(),
                ticket.getExitTime() != null ? ticket.getExitTime() : LocalDateTime.now());
        hours = Math.max(1, hours); // minimum 1 hour

        double rate = hourlyRates.get(ticket.getVehicle().getType());
        return hours * rate;
    }
}

// ──── Payment (Strategy Pattern) ────
public interface PaymentMethod {
    boolean pay(double amount);
}

public class CashPayment implements PaymentMethod {
    public boolean pay(double amount) {
        // Process cash payment
        return true;
    }
}

public class CardPayment implements PaymentMethod {
    public boolean pay(double amount) {
        // Process card payment
        return true;
    }
}

// ──── Parking Lot (Singleton + Facade) ────
public class ParkingLot {
    private static ParkingLot instance;

    private final String name;
    private final List<ParkingFloor> floors;
    private final Map<String, ParkingTicket> activeTickets; // ticketId → ticket
    private final PricingStrategy pricingStrategy;

    private ParkingLot(String name, int numFloors, PricingStrategy pricingStrategy) {
        this.name = name;
        this.floors = new ArrayList<>();
        this.activeTickets = new ConcurrentHashMap<>();
        this.pricingStrategy = pricingStrategy;

        for (int i = 1; i <= numFloors; i++) {
            floors.add(new ParkingFloor(i));
        }
    }

    public static synchronized ParkingLot getInstance(String name, int numFloors,
            PricingStrategy pricingStrategy) {
        if (instance == null) {
            instance = new ParkingLot(name, numFloors, pricingStrategy);
        }
        return instance;
    }

    // Entry: Park vehicle and issue ticket
    public ParkingTicket parkVehicle(Vehicle vehicle) {
        SpotSize requiredSize = vehicle.getRequiredSpotSize();

        for (ParkingFloor floor : floors) {
            Optional<ParkingSpot> spotOpt = floor.findAvailableSpot(requiredSize);
            if (spotOpt.isPresent()) {
                ParkingSpot spot = spotOpt.get();
                if (spot.park(vehicle)) {
                    ParkingTicket ticket = new ParkingTicket(vehicle, spot);
                    activeTickets.put(ticket.getTicketId(), ticket);
                    return ticket;
                }
            }
        }
        throw new ParkingFullException("No available spot for " + vehicle.getType());
    }

    // Exit: Unpark vehicle and process payment
    public double unparkVehicle(String ticketId, PaymentMethod paymentMethod) {
        ParkingTicket ticket = activeTickets.get(ticketId);
        if (ticket == null) {
            throw new InvalidTicketException(ticketId);
        }

        ticket.setExitTime(LocalDateTime.now());
        double amount = pricingStrategy.calculatePrice(ticket);

        if (paymentMethod.pay(amount)) {
            ticket.getSpot().unpark();
            ticket.setStatus(TicketStatus.PAID);
            ticket.setAmountPaid(amount);
            activeTickets.remove(ticketId);
            return amount;
        }

        throw new PaymentFailedException("Payment failed for ticket " + ticketId);
    }

    // Display available spots
    public Map<Integer, Map<SpotSize, Long>> getAvailability() {
        Map<Integer, Map<SpotSize, Long>> availability = new LinkedHashMap<>();
        for (ParkingFloor floor : floors) {
            Map<SpotSize, Long> floorAvailability = new EnumMap<>(SpotSize.class);
            for (SpotSize size : SpotSize.values()) {
                floorAvailability.put(size, floor.getAvailableSpotCount(size));
            }
            availability.put(floor.getFloorNumber(), floorAvailability);
        }
        return availability;
    }
}

// ──── Demo Usage ────
public class ParkingLotDemo {
    public static void main(String[] args) {
        ParkingLot lot = ParkingLot.getInstance("Mall Parking", 3,
                new HourlyPricingStrategy());

        // Add spots to floors (normally done during initialization)

        // Car enters
        Vehicle car = new Car("KA-01-AB-1234");
        ParkingTicket ticket = lot.parkVehicle(car);
        System.out.println("Parked! Ticket: " + ticket.getTicketId());

        // Car exits after some time
        double amount = lot.unparkVehicle(ticket.getTicketId(), new CardPayment());
        System.out.println("Amount paid: ₹" + amount);
    }
}
