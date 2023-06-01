package hardwar.branch.prediction.judged.SAs;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class SAs implements BranchPredictor {

    private final int branchInstructionSize;
    private final int KSize;
    private final ShiftRegister SC;
    private final RegisterBank PSBHR; // per set branch history register
    private final Cache<Bit[], Bit[]> PSPHT; // per set predication history table
    private final HashMode hashMode;

    public SAs() {
        this(4, 2, 8, 4, HashMode.XOR);
    }

    public SAs(int BHRSize, int SCSize, int branchInstructionSize, int KSize, HashMode hashMode) {
        // TODO: complete the constructor
        this.branchInstructionSize = branchInstructionSize;
        this.KSize = KSize;
        this.hashMode = HashMode.XOR;

        PSBHR = new RegisterBank(KSize , BHRSize);

        PSPHT = new PerAddressPredictionHistoryTable(KSize,(int)Math.pow(2,BHRSize),SCSize);

        // Initialize the SC register
        SC = new SIPORegister("ali", SCSize, null);
    }

    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        // TODO: complete Task 1

        ShiftRegister BHR = PSBHR.read(getAddressLine(branchInstruction.getInstructionAddress()));
        SC.load(PSPHT.setDefault(getCacheEntry(branchInstruction.getInstructionAddress() , BHR.read()), getDefaultBlock()));
        return BranchResult.of(SC.read()[0].getValue());    }

    @Override
    public void update(BranchInstruction branchInstruction, BranchResult actual) {
        ShiftRegister BHR = PSBHR.read(getAddressLine(branchInstruction.getInstructionAddress()));
        SC.load(CombinationalLogic.count(SC.read(), BranchResult.isTaken(actual), CountMode.SATURATING));
        PSPHT.put(getCacheEntry(branchInstruction.getInstructionAddress(),BHR.read()), SC.read());

        BHR.insert(Bit.of(BranchResult.isTaken(actual)));
        PSBHR.write(getAddressLine(branchInstruction.getInstructionAddress()) , BHR.read());
    }


    private Bit[] getAddressLine(Bit[] branchAddress) {
        // hash the branch address
        return CombinationalLogic.hash(branchAddress, KSize, hashMode);
    }

    private Bit[] getCacheEntry(Bit[] branchAddress, Bit[] BHRValue) {
        // Concatenate the branch address bits with the BHR bits
        Bit[] cacheEntry = new Bit[branchAddress.length + BHRValue.length];
        System.arraycopy(branchAddress, 0, cacheEntry, 0, KSize);
        System.arraycopy(BHRValue, 0, cacheEntry, branchAddress.length, BHRValue.length);
        return cacheEntry;
    }

    /**
     * @return a zero series of bits as default value of cache block
     */
    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }

    @Override
    public String monitor() {
        return null;
    }
}
