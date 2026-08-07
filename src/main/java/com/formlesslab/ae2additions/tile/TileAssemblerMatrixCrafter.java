package com.formlesslab.ae2additions.tile;

import ae2.api.config.Actionable;
import ae2.api.config.PowerMultiplier;
import ae2.api.crafting.IAssemblerPattern;
import ae2.api.crafting.IPatternDetails;
import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.api.networking.ticking.IGridTickable;
import ae2.api.networking.ticking.TickRateModulation;
import ae2.api.networking.ticking.TickingRequest;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import com.formlesslab.ae2additions.AppliedAdditions;
import com.formlesslab.ae2additions.init.Configurations;
import com.formlesslab.ae2additions.me.cluster.ClusterAssemblerMatrix;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class TileAssemblerMatrixCrafter extends TileAssemblerMatrixFunction implements IGridTickable {

    private static final int INPUT_SLOTS = 9;
    private static final int COOL_TIME = 5 * 20;
    private static final String NBT_QUEUE = "craftQueue";
    private static final String NBT_OUTPUT_BUFFER = "outputBuffer";
    private static final String NBT_PROGRESS = "queueProgress";
    private static final String NBT_COOLDOWN = "outputCooldown";
    private static final String NBT_CRAFT_COUNT = "craftCount";
    private static final String NBT_INPUTS = "inputs";
    private static final String NBT_OUTPUTS = "outputs";
    private static final Container NULL_CONTAINER = new Container() {
        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return false;
        }
    };

    private final InventoryCrafting craftingInv = new InventoryCrafting(NULL_CONTAINER, 3, 3);
    private final List<QueuedCraft> craftQueue = new ArrayList<>();
    private final List<GenericStack> outputBuffer = new ArrayList<>();
    private double savedQueueProgress;
    private int outputCooldown;

    public TileAssemblerMatrixCrafter() {
        super(Configurations.ASSEMBLER_MATRIX.crafterIdlePowerUsage);
        this.getMainNode().addService(IGridTickable.class, this);
    }

    public static int getMaxQueueSize() {
        return Configurations.ASSEMBLER_MATRIX.crafterQueueSize;
    }

    public static int getMaxParallel() {
        return Configurations.ASSEMBLER_MATRIX.crafterParallelism;
    }

    private static KeyCounter[] copyInputHolder(KeyCounter[] inputHolder) {
        KeyCounter[] copy = new KeyCounter[inputHolder.length];
        for (int i = 0; i < inputHolder.length; i++) {
            copy[i] = new KeyCounter();
            copy[i].addAll(inputHolder[i]);
        }
        return copy;
    }

    private static List<GenericStack> flattenInputs(KeyCounter[] inputHolder) {
        KeyCounter merged = new KeyCounter();
        for (KeyCounter counter : inputHolder) {
            merged.addAll(counter);
        }
        return toGenericStacks(merged);
    }

    private static List<GenericStack> mergeStacks(List<GenericStack> stacks) {
        KeyCounter merged = new KeyCounter();
        for (GenericStack stack : stacks) {
            if (stack != null && stack.amount() > 0) {
                merged.add(stack.what(), stack.amount());
            }
        }
        return toGenericStacks(merged);
    }

    private static List<GenericStack> toGenericStacks(KeyCounter counter) {
        List<GenericStack> result = new ArrayList<>();
        for (var entry : counter) {
            if (entry.getLongValue() > 0) {
                result.add(new GenericStack(entry.getKey(), entry.getLongValue()));
            }
        }
        return result;
    }

    private static List<GenericStack> readStacks(NBTTagCompound data, String key) {
        if (!data.hasKey(key, Constants.NBT.TAG_LIST)) {
            return new ArrayList<>();
        }
        List<GenericStack> result = new ArrayList<>();
        for (GenericStack stack : GenericStack.readList(data.getTagList(key, Constants.NBT.TAG_COMPOUND))) {
            if (stack != null && stack.amount() > 0) {
                result.add(stack);
            }
        }
        return mergeStacks(result);
    }

    private static long getTemplateAmount(IPatternDetails.IInput input, AEKey key) {
        for (GenericStack possibleInput : input.possibleInputs()) {
            if (possibleInput.what().equals(key)) {
                return possibleInput.amount();
            }
        }
        return 1;
    }

    private static long saturatedMultiply(long amount, int multiplier) {
        if (amount <= 0 || multiplier <= 0) {
            return 0;
        }
        return amount > Long.MAX_VALUE / multiplier ? Long.MAX_VALUE : amount * multiplier;
    }

    public int getQueuedJobCount() {
        return this.craftQueue.size();
    }

    public int getUsedParallel() {
        long used = 0;
        for (QueuedCraft craft : this.craftQueue) {
            used += craft.craftCount;
        }
        return used >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) used;
    }

    public boolean hasQueueSpace() {
        return this.craftQueue.size() < getMaxQueueSize();
    }

    public boolean hasQueuedJobs() {
        return !this.craftQueue.isEmpty();
    }

    public boolean hasBufferedOutputs() {
        return !this.outputBuffer.isEmpty();
    }

    public boolean pushJob(IPatternDetails patternDetails, KeyCounter[] inputHolder, int craftCount) {
        if (!this.hasQueueSpace() || craftCount <= 0 || !(patternDetails instanceof IAssemblerPattern pattern)) {
            return false;
        }

        QueuedCraft queuedCraft = this.createQueuedCraft(pattern, inputHolder, craftCount);
        if (queuedCraft == null) {
            return false;
        }

        for (KeyCounter counter : inputHolder) {
            counter.clear();
        }
        this.craftQueue.add(queuedCraft);
        this.savedQueueProgress = this.cluster == null ? 0 : this.cluster.getQueueProgress();
        this.saveChanges();
        this.wakeForWork();
        return true;
    }

    public void completeQueuedJobs() {
        if (this.craftQueue.isEmpty()) {
            return;
        }
        for (QueuedCraft craft : this.craftQueue) {
            this.mergeIntoOutputBuffer(craft.outputs);
        }
        this.craftQueue.clear();
        this.savedQueueProgress = 0;
        this.saveChanges();
        this.wakeForWork();
    }

    public void cancelQueuedJobs() {
        if (this.craftQueue.isEmpty()) {
            return;
        }
        for (QueuedCraft craft : this.craftQueue) {
            this.mergeIntoOutputBuffer(craft.inputs);
        }
        this.craftQueue.clear();
        this.savedQueueProgress = 0;
        this.saveChanges();
        this.wakeForWork();
    }

    public double getSavedQueueProgress() {
        return this.savedQueueProgress;
    }

    public void setSavedQueueProgress(double progress) {
        this.savedQueueProgress = this.craftQueue.isEmpty() ? 0 : Math.max(0, progress);
    }

    public int advanceQueueTimer(int speedCores, int ticksSinceLastCall) {
        return switch (Math.min(speedCores, 5)) {
            case 1 -> this.usePower(ticksSinceLastCall, 26, 1.3);
            case 2 -> this.usePower(ticksSinceLastCall, 34, 1.7);
            case 3 -> this.usePower(ticksSinceLastCall, 40, 2.0);
            case 4 -> this.usePower(ticksSinceLastCall, 50, 2.5);
            case 5 -> this.usePower(ticksSinceLastCall, 100, 5.0);
            default -> this.usePower(ticksSinceLastCall, 20, 1.0);
        };
    }

    public void wakeForWork() {
        this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        data.setTag(NBT_QUEUE, this.writeQueue());
        data.setTag(NBT_OUTPUT_BUFFER, GenericStack.writeList(this.outputBuffer));
        data.setInteger(NBT_COOLDOWN, this.outputCooldown);
        double progress = this.cluster != null && this.cluster.hasQueuedJobs() ? this.cluster.getQueueProgress() : this.savedQueueProgress;
        data.setDouble(NBT_PROGRESS, progress);
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.craftQueue.clear();
        this.outputBuffer.clear();
        this.outputCooldown = Math.max(0, data.getInteger(NBT_COOLDOWN));
        this.savedQueueProgress = Math.max(0, data.getDouble(NBT_PROGRESS));

        this.readQueue(data);
        this.mergeIntoOutputBuffer(readStacks(data, NBT_OUTPUT_BUFFER));
    }

    @Override
    public void add(ClusterAssemblerMatrix cluster) {
        cluster.addCrafter(this);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        boolean awake = this.hasQueuedJobs() || this.hasBufferedOutputs();
        return new TickingRequest(1, 1, !awake);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (this.cluster == null) {
            return TickRateModulation.SLEEP;
        }

        TickRateModulation rate = TickRateModulation.SLEEP;
        if (this.cluster.hasQueuedJobs()) {
            rate = this.cluster.tickCraftingQueue(this, ticksSinceLastCall);
        }

        TickRateModulation outputRate = this.tickOutputBuffer(ticksSinceLastCall);
        if (outputRate.ordinal() > rate.ordinal()) {
            rate = outputRate;
        }
        if (this.hasQueuedJobs() && TickRateModulation.FASTER.ordinal() > rate.ordinal()) {
            rate = TickRateModulation.FASTER;
        }
        if (!this.hasQueuedJobs() && !this.hasBufferedOutputs()) {
            return TickRateModulation.SLEEP;
        }
        return rate;
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops) {
        super.addAdditionalDrops(drops);
        for (QueuedCraft craft : this.craftQueue) {
            this.addGenericStackDrops(craft.inputs, drops);
        }
        this.addGenericStackDrops(this.outputBuffer, drops);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.craftQueue.clear();
        this.outputBuffer.clear();
        this.savedQueueProgress = 0;
        this.outputCooldown = 0;
    }

    private QueuedCraft createQueuedCraft(IAssemblerPattern pattern, KeyCounter[] inputHolder, int craftCount) {
        KeyCounter[] representativeInput = copyInputHolder(inputHolder);
        ItemStack[] sparseInputs = new ItemStack[INPUT_SLOTS];
        try {
            pattern.fillCraftingGrid(representativeInput, (slot, stack) -> sparseInputs[slot] = stack);
            for (int slot = 0; slot < sparseInputs.length; slot++) {
                this.craftingInv.setInventorySlotContents(slot, sparseInputs[slot] == null ? ItemStack.EMPTY : sparseInputs[slot]);
            }

            ItemStack assembled = pattern.assemble(this.craftingInv, this.world);
            GenericStack mainOutput = GenericStack.fromItemStack(assembled);
            if (mainOutput == null) {
                return null;
            }

            List<GenericStack> outputs = new ArrayList<>();
            outputs.add(new GenericStack(mainOutput.what(), saturatedMultiply(mainOutput.amount(), craftCount)));
            this.collectRemainders(pattern, inputHolder, outputs);
            return new QueuedCraft(craftCount, flattenInputs(inputHolder), mergeStacks(outputs));
        } catch (RuntimeException e) {
            AppliedAdditions.LOGGER.warn("Unable to queue assembler matrix pattern {}", pattern.getDefinition(), e);
            return null;
        }
    }

    private void collectRemainders(IAssemblerPattern pattern, KeyCounter[] inputHolder, List<GenericStack> outputs) {
        KeyCounter remainders = new KeyCounter();
        IPatternDetails.IInput[] inputs = pattern.getInputs();
        for (int slot = 0; slot < Math.min(inputs.length, inputHolder.length); slot++) {
            for (var entry : inputHolder[slot]) {
                var remainder = inputs[slot].getRemainingKey(entry.getKey());
                if (remainder == null) {
                    continue;
                }
                long templateAmount = getTemplateAmount(inputs[slot], entry.getKey());
                if (templateAmount > 0) {
                    remainders.add(remainder, entry.getLongValue() / templateAmount);
                }
            }
        }
        for (var entry : remainders) {
            if (entry.getLongValue() > 0) {
                outputs.add(new GenericStack(entry.getKey(), entry.getLongValue()));
            }
        }
    }

    private TickRateModulation tickOutputBuffer(int ticksSinceLastCall) {
        if (this.outputBuffer.isEmpty()) {
            return TickRateModulation.SLEEP;
        }
        if (this.outputCooldown > 0) {
            this.outputCooldown = Math.max(0, this.outputCooldown - ticksSinceLastCall);
            return TickRateModulation.SAME;
        }

        IGrid grid = this.getMainNode().getGrid();
        if (grid == null) {
            return TickRateModulation.SAME;
        }

        List<GenericStack> remaining = new ArrayList<>();
        boolean insertedAny = false;
        for (GenericStack stack : this.outputBuffer) {
            long inserted = grid.getStorageService().getInventory().insert(stack.what(), stack.amount(), Actionable.MODULATE, this.cluster.getSrc());
            if (inserted > 0) {
                insertedAny = true;
            }
            if (inserted < stack.amount()) {
                remaining.add(new GenericStack(stack.what(), stack.amount() - inserted));
            }
        }
        this.outputBuffer.clear();
        this.outputBuffer.addAll(remaining);
        if (!this.outputBuffer.isEmpty()) {
            this.outputCooldown = COOL_TIME;
        }
        if (insertedAny || !remaining.isEmpty()) {
            this.saveChanges();
        }
        if (this.outputBuffer.isEmpty()) {
            return TickRateModulation.IDLE;
        }
        return insertedAny ? TickRateModulation.FASTER : TickRateModulation.SAME;
    }

    private int usePower(int ticksPassed, int bonusValue, double acceleratorTax) {
        IGrid grid = this.getMainNode().getGrid();
        if (grid == null) {
            return 0;
        }
        double safePower = Math.min(ticksPassed * bonusValue * acceleratorTax, 5000);
        return (int) (grid.getEnergyService().extractAEPower(safePower, Actionable.MODULATE, PowerMultiplier.CONFIG) / acceleratorTax);
    }

    private void mergeIntoOutputBuffer(List<GenericStack> stacks) {
        if (stacks.isEmpty()) {
            return;
        }
        List<GenericStack> merged = new ArrayList<>(this.outputBuffer.size() + stacks.size());
        merged.addAll(this.outputBuffer);
        merged.addAll(stacks);
        this.outputBuffer.clear();
        this.outputBuffer.addAll(mergeStacks(merged));
    }

    private NBTTagList writeQueue() {
        NBTTagList result = new NBTTagList();
        for (QueuedCraft craft : this.craftQueue) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger(NBT_CRAFT_COUNT, craft.craftCount);
            entry.setTag(NBT_INPUTS, GenericStack.writeList(craft.inputs));
            entry.setTag(NBT_OUTPUTS, GenericStack.writeList(craft.outputs));
            result.appendTag(entry);
        }
        return result;
    }

    private void readQueue(NBTTagCompound data) {
        if (!data.hasKey(NBT_QUEUE, Constants.NBT.TAG_LIST)) {
            return;
        }
        NBTTagList queue = data.getTagList(NBT_QUEUE, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < queue.tagCount(); i++) {
            NBTTagCompound entry = queue.getCompoundTagAt(i);
            int craftCount = entry.getInteger(NBT_CRAFT_COUNT);
            List<GenericStack> inputs = readStacks(entry, NBT_INPUTS);
            List<GenericStack> outputs = readStacks(entry, NBT_OUTPUTS);
            if (craftCount > 0 && !outputs.isEmpty()) {
                this.craftQueue.add(new QueuedCraft(craftCount, inputs, outputs));
            } else {
                this.mergeIntoOutputBuffer(inputs);
            }
        }
        if (this.craftQueue.isEmpty()) {
            this.savedQueueProgress = 0;
        }
    }

    private void addGenericStackDrops(List<GenericStack> stacks, List<ItemStack> drops) {
        if (this.world == null) {
            return;
        }
        for (GenericStack stack : stacks) {
            stack.what().addDrops(stack.amount(), drops, this.world, this.pos);
        }
    }

    private record QueuedCraft(int craftCount, List<GenericStack> inputs, List<GenericStack> outputs) {
    }
}
