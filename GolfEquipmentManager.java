import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

// ==========================================
// 1. NGOẠI LỆ TỰ ĐỊNH NGHĨA (CUSTOM EXCEPTIONS)
// ==========================================
class InvalidEquipmentDataException extends Exception {
    public InvalidEquipmentDataException(String message) {
        super(message);
    }
}

class EntityNotFoundException extends Exception {
    public EntityNotFoundException(String message) {
        super(message);
    }
}

// ==========================================
// 2. INTERFACES (CÓ DEFAULT METHOD)
// ==========================================
interface Printable {
    void printDetails();
    
    default String getFormattedHeader() {
        return "=== THÔNG TIN THIẾT BỊ SÂN GOLF ===";
    }
}

interface Exportable {
    String toCsvRow();
}

// ==========================================
// 3. STRATEGY PATTERN (CHIẾN LƯỢC BẢO TRÌ)
// ==========================================
interface MaintenanceStrategy {
    boolean needsMaintenance(Equipment equipment, MaintenanceRule rule, double lastMaintenanceHour);
}

class StandardMaintenanceStrategy implements MaintenanceStrategy {
    @Override
    public boolean needsMaintenance(Equipment equipment, MaintenanceRule rule, double lastMaintenanceHour) {
        double hoursSinceLast = equipment.getCurrentHours() - lastMaintenanceHour;
        return equipment.getCurrentHours() >= rule.getIntervalHours() && (hoursSinceLast >= rule.getIntervalHours() || lastMaintenanceHour == 0);
    }
}

// ==========================================
// 4. KẾ THỪA 3 TẦNG & LỚP TRỪU TƯỢNG (ABSTRACT)
// ==========================================

// Tầng 1: Lớp trừu tượng cơ sở
abstract class BaseEntity implements Printable, Exportable, Serializable {
    public static final String DEFAULT_STATUS = "HOAT_DONG";
    private static int totalEntitiesCreated = 0;

    private String id;
    private String name;

    public BaseEntity() {
        this("UNKNOWN", "Chưa đặt tên");
    }

    public BaseEntity(String id, String name) {
        this.id = id;
        this.name = name;
        totalEntitiesCreated++;
    }

    public static int getTotalEntitiesCreated() {
        return totalEntitiesCreated;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public abstract String getEntityType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

// Tầng 2: Lớp thiết bị trừu tượng
abstract class Equipment extends BaseEntity {
    private String modelName;
    private String vehicleNo;
    private String checkDate;
    private double currentHours;

    public Equipment(String modelName, String vehicleNo, String checkDate, double currentHours) throws InvalidEquipmentDataException {
        super(modelName.trim() + "_" + vehicleNo.trim(), modelName.trim());
        setModelName(modelName);
        setVehicleNo(vehicleNo);
        setCheckDate(checkDate);
        setCurrentHours(currentHours);
    }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) throws InvalidEquipmentDataException {
        if (modelName == null || modelName.trim().isEmpty()) {
            throw new InvalidEquipmentDataException("Tên Model không được để trống!");
        }
        this.modelName = modelName.trim();
    }

    public String getVehicleNo() { return vehicleNo; }
    public void setVehicleNo(String vehicleNo) throws InvalidEquipmentDataException {
        if (vehicleNo == null || vehicleNo.trim().isEmpty()) {
            throw new InvalidEquipmentDataException("Số xe không được để trống!");
        }
        this.vehicleNo = vehicleNo.trim();
    }

    public String getCheckDate() { return checkDate; }
    public void setCheckDate(String checkDate) throws InvalidEquipmentDataException {
        if (checkDate == null || checkDate.trim().isEmpty()) {
            throw new InvalidEquipmentDataException("Ngày kiểm tra không được để trống!");
        }
        this.checkDate = checkDate.trim();
    }

    public double getCurrentHours() { return currentHours; }
    public void setCurrentHours(double currentHours) throws InvalidEquipmentDataException {
        if (currentHours < 0) {
            throw new InvalidEquipmentDataException("Số giờ vận hành không được âm!");
        }
        this.currentHours = currentHours;
    }

    public void updateHours(double additionalHours) throws InvalidEquipmentDataException {
        if (additionalHours < 0) throw new InvalidEquipmentDataException("Số giờ thêm không được âm!");
        setCurrentHours(this.currentHours + additionalHours);
    }

    public void updateHours(int additionalHours, boolean isReset) throws InvalidEquipmentDataException {
        if (isReset) {
            setCurrentHours(additionalHours);
        } else {
            updateHours((double) additionalHours);
        }
    }

    @Override
    public void printDetails() {
        System.out.println(getFormattedHeader());
        System.out.printf("Mã: %s | Model: %s | Xe số: %s | Ngày Checklist: %s | Giờ HĐ: %.1fh\n",
                getId(), modelName, vehicleNo, checkDate, currentHours);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - Xe số: %s - Ngày: %s - Giờ HĐ: %.1fh",
                getId(), modelName, vehicleNo, checkDate, currentHours);
    }
}

// Tầng 3: Lớp con cụ thể
class MowingEquipment extends Equipment {
    private double cuttingWidthMeters;

    public MowingEquipment(String modelName, String vehicleNo, String checkDate, double currentHours, double cuttingWidthMeters) throws InvalidEquipmentDataException {
        super(modelName, vehicleNo, checkDate, currentHours);
        setCuttingWidthMeters(cuttingWidthMeters);
    }

    public double getCuttingWidthMeters() { return cuttingWidthMeters; }
    public void setCuttingWidthMeters(double cuttingWidthMeters) throws InvalidEquipmentDataException {
        if (cuttingWidthMeters <= 0) throw new InvalidEquipmentDataException("Bề rộng cắt phải lớn hơn 0!");
        this.cuttingWidthMeters = cuttingWidthMeters;
    }

    @Override
    public String getEntityType() { return "Xe Cắt Cỏ (Mowing Equipment)"; }

    @Override
    public String toCsvRow() {
        return String.join(",", "MOWING", getModelName(), getVehicleNo(), getCheckDate(),
                String.valueOf(getCurrentHours()), String.valueOf(cuttingWidthMeters));
    }
}

class UtilityVehicle extends Equipment {
    private double payloadCapacityKg;

    public UtilityVehicle(String modelName, String vehicleNo, String checkDate, double currentHours, double payloadCapacityKg) throws InvalidEquipmentDataException {
        super(modelName, vehicleNo, checkDate, currentHours);
        setPayloadCapacityKg(payloadCapacityKg);
    }

    public double getPayloadCapacityKg() { return payloadCapacityKg; }
    public void setPayloadCapacityKg(double payloadCapacityKg) throws InvalidEquipmentDataException {
        if (payloadCapacityKg <= 0) throw new InvalidEquipmentDataException("Tải trọng phải lớn hơn 0!");
        this.payloadCapacityKg = payloadCapacityKg;
    }

    @Override
    public String getEntityType() { return "Xe Tiện Ích (Utility Vehicle)"; }

    @Override
    public String toCsvRow() {
        return String.join(",", "UTILITY", getModelName(), getVehicleNo(), getCheckDate(),
                String.valueOf(getCurrentHours()), String.valueOf(payloadCapacityKg));
    }
}

// ==========================================
// 5. THỰC THỂ PHỤ TÙNG & NHẬT KÝ BẢO TRÌ
// ==========================================
class Part implements Exportable, Serializable {
    private String partCode;
    private String partName;

    public Part(String partCode, String partName) {
        this.partCode = partCode;
        this.partName = partName;
    }

    public String getPartCode() { return partCode; }
    public String getPartName() { return partName; }

    @Override
    public String toCsvRow() { return partCode + "," + partName; }
}

class MaintenanceRule implements Serializable {
    private String modelName;
    private double intervalHours;
    private Part part;

    public MaintenanceRule(String modelName, double intervalHours, String partCode, String partName) {
        this.modelName = modelName;
        this.intervalHours = intervalHours;
        this.part = new Part(partCode, partName);
    }

    public String getModelName() { return modelName; }
    public double getIntervalHours() { return intervalHours; }
    public Part getPart() { return part; }
}

class MaintenanceLog implements Exportable, Serializable {
    private String equipmentId;
    private Part part;
    private double hourAtReplacement;

    public MaintenanceLog(String equipmentId, String partCode, String partName, double hourAtReplacement) {
        this.equipmentId = equipmentId;
        this.part = new Part(partCode, partName);
        this.hourAtReplacement = hourAtReplacement;
    }

    public String getEquipmentId() { return equipmentId; }
    public Part getPart() { return part; }
    public double getHourAtReplacement() { return hourAtReplacement; }

    @Override
    public String toCsvRow() {
        return String.join(",", equipmentId, part.getPartCode(), part.getPartName(), String.valueOf(hourAtReplacement));
    }
}

class MaintenanceDetail {
    private String partName;
    private long historyCount;

    public MaintenanceDetail(String partName, long historyCount) {
        this.partName = partName;
        this.historyCount = historyCount;
    }

    public String getPartName() { return partName; }
    public long getHistoryCount() { return historyCount; }
}

// ==========================================
// 6. FACTORY PATTERN
// ==========================================
class EquipmentFactory {
    public static Equipment createEquipment(String modelName, String vehicleNo, String checkDate, double hours) throws InvalidEquipmentDataException {
        String modelUpper = modelName.toUpperCase();
        if (modelUpper.contains("MULTIPRO") || modelUpper.contains("WORKMAN")) {
            return new UtilityVehicle(modelName, vehicleNo, checkDate, hours, 500.0);
        } else {
            return new MowingEquipment(modelName, vehicleNo, checkDate, hours, 1.8);
        }
    }
}

// ==========================================
// 7. FILE I/O (LƯU VÀ ĐỌC CSV)
// ==========================================
class DataStorageService {
    private static final String LOG_FILE = "maintenance_logs.csv";

    public static void saveLogs(List<MaintenanceLog> list) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE))) {
            for (MaintenanceLog log : list) {
                writer.println(log.toCsvRow());
            }
        } catch (IOException e) {
            System.err.println("Lỗi ghi file CSV: " + e.getMessage());
        }
    }

    public static List<MaintenanceLog> loadLogs() {
        List<MaintenanceLog> list = new ArrayList<>();
        File file = new File(LOG_FILE);
        if (!file.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",");
                if (p.length >= 4) {
                    list.add(new MaintenanceLog(p[0], p[1], p[2], Double.parseDouble(p[3])));
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi đọc file CSV: " + e.getMessage());
        }
        return list;
    }
}

// ==========================================
// 8. SERVICE LOGIC
// ==========================================
class MaintenanceService {
    private List<MaintenanceRule> rules = new ArrayList<>();
    private List<MaintenanceLog> logs = new ArrayList<>();
    private MaintenanceStrategy strategy;

    public MaintenanceService() {
        this.strategy = new StandardMaintenanceStrategy();
        this.logs = DataStorageService.loadLogs();
        initRules();

        if (logs.isEmpty()) {
            initSampleLogs();
        }
    }

    private void initRules() {
        String[] allModels = {"Reelmaster 6500", "Reelmaster 3100", "Triflex 3400", "Multipro 5800", "Flex 2100", "Greenmaster 1600"};
        for (String model : allModels) {
            rules.add(new MaintenanceRule(model, 100, "OIL", "Dau dong co"));
            rules.add(new MaintenanceRule(model, 200, "OIL_FILTER", "Loc dau dong co"));
            rules.add(new MaintenanceRule(model, 400, "FUEL_FILTER", "Loc nhien lieu"));
            rules.add(new MaintenanceRule(model, 400, "AIR_FILTER", "Ruot loc gio chinh"));
            rules.add(new MaintenanceRule(model, 800, "TRANSAXLE_OIL", "Dau cau / Hop so"));
        }
    }

    private void initSampleLogs() {
        logs.add(new MaintenanceLog("Reelmaster 6500_So 1", "OIL", "Dau dong co", 100));
        logs.add(new MaintenanceLog("Reelmaster 6500_So 1", "OIL", "Dau dong co", 200));
        logs.add(new MaintenanceLog("Reelmaster 6500_So 1", "OIL_FILTER", "Loc dau dong co", 200));
        logs.add(new MaintenanceLog("Reelmaster 6500_So 1", "OIL", "Dau dong co", 300));
        logs.add(new MaintenanceLog("Flex 2100_So 1", "OIL", "Dau dong co", 100));
        DataStorageService.saveLogs(logs);
    }

    public List<MaintenanceDetail> processEquipment(Equipment eq) {
        List<MaintenanceDetail> details = new ArrayList<>();

        List<MaintenanceRule> modelRules = rules.stream()
                .filter(r -> r.getModelName().equalsIgnoreCase(eq.getModelName()))
                .collect(Collectors.toList());

        for (MaintenanceRule rule : modelRules) {
            long count = logs.stream()
                    .filter(l -> l.getEquipmentId().equalsIgnoreCase(eq.getId()))
                    .filter(l -> l.getPart().getPartCode().equals(rule.getPart().getPartCode()))
                    .count();

            Optional<MaintenanceLog> lastLog = logs.stream()
                    .filter(l -> l.getEquipmentId().equalsIgnoreCase(eq.getId()))
                    .filter(l -> l.getPart().getPartCode().equals(rule.getPart().getPartCode()))
                    .max(Comparator.comparingDouble(MaintenanceLog::getHourAtReplacement));

            double lastHour = lastLog.map(MaintenanceLog::getHourAtReplacement).orElse(0.0);

            if (strategy.needsMaintenance(eq, rule, lastHour)) {
                details.add(new MaintenanceDetail(rule.getPart().getPartName(), count));
            }
        }
        return details;
    }
}

// ==========================================
// 9. CHƯƠNG TRÌNH CHÍNH (MAIN)
// ==========================================
public class GolfEquipmentManager {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        MaintenanceService service = new MaintenanceService();
        Map<Equipment, List<MaintenanceDetail>> resultMap = new LinkedHashMap<>();

        System.out.println("-> Go 'done' hoac nhan Enter tren dong trong de DUNG VA XEM KET QUA.\n");

        int lineCount = 1;
        while (true) {
            System.out.print("Nhap dong " + lineCount + ": ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("done") || input.isEmpty()) {
                break;
            }

            String[] parts = input.split("-");

            if (parts.length < 4) {
                System.out.println(" ❌ Sai dinh dang! Mau chuoi chuan: Ten thiet bi-So Xe-Ngay Checklist-So Gio Hoat Dong\n");
                continue;
            }

            String modelName = parts[0].trim();
            String vehicleNo = parts[1].trim();
            String checkDate = parts[2].trim();
            double hours = 0;

            try {
                hours = Double.parseDouble(parts[3].trim());
                Equipment eq = EquipmentFactory.createEquipment(modelName, vehicleNo, checkDate, hours);
                List<MaintenanceDetail> details = service.processEquipment(eq);
                resultMap.put(eq, details);
                lineCount++;
            } catch (NumberFormatException e) {
                System.out.println(" ❌ So gio hoat dong phai la mot so hop le!\n");
            } catch (InvalidEquipmentDataException e) {
                System.out.println(" ❌ Du lieu khong hop le: " + e.getMessage() + "\n");
            }
        }

        // IN BẢNG KẾT QUẢ ĐẦU RA CHUẨN ĐỊNH DẠNG HÌNH ẢNH
        System.out.println("\n==================================================================================================================================");
        System.out.println("                                               BANG TONG HOP CHESTLIST BAO TRI XE");
        System.out.println("==================================================================================================================================");
        System.out.printf("| %-20s | %-7s | %-14s | %-20s | %-25s | %-23s |\n", 
                "Ten Thiet Bi", "So Xe", "Ngay Chestlist", "So Gio Hoat Dong", "Noi Dung Can Thay The", "Lich Su So Lan Thay");
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------");

        if (resultMap.isEmpty()) {
            System.out.println("|                                        KHONG CO DU LIEU XE DUOC NHAP CAP NHAT                                                  |");
        } else {
            for (Map.Entry<Equipment, List<MaintenanceDetail>> entry : resultMap.entrySet()) {
                Equipment eq = entry.getKey();
                List<MaintenanceDetail> details = entry.getValue();

                if (details.isEmpty()) {
                    System.out.printf("| %-20s | %-7s | %-14s | %-20.1f | %-25s | %-23s |\n",
                            eq.getModelName(), eq.getVehicleNo(), eq.getCheckDate(), eq.getCurrentHours(),
                            "Hoat dong binh thuong", "-");
                } else {
                    for (int i = 0; i < details.size(); i++) {
                        MaintenanceDetail item = details.get(i);
                        String model = (i == 0) ? eq.getModelName() : "";
                        String vehicle = (i == 0) ? eq.getVehicleNo() : "";
                        String date = (i == 0) ? eq.getCheckDate() : "";
                        String hour = (i == 0) ? String.format("%.1f", eq.getCurrentHours()) : "";

                        String historyStr = "Da thay " + item.getHistoryCount() + " lan truoc do";

                        System.out.printf("| %-20s | %-7s | %-14s | %-20s | %-25s | %-23s |\n",
                                model, vehicle, date, hour, item.getPartName(), historyStr);
                    }
                }
                System.out.println("----------------------------------------------------------------------------------------------------------------------------------");
            }
        }

        scanner.close();
    }
}