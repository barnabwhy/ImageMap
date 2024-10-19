package cc.barnab.core.gui;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;

public class ChestGUIScreenHandlerFactory extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(9 * 6, ItemStack.EMPTY);

    private final boolean[] slotLockState = new boolean[9*6];

    private final HashMap<Integer, ChestGUIClickCallback> clickCallbacks = new HashMap<>();

    private final String name;

    public ChestGUIScreenHandlerFactory(String name) {
        super(BlockEntityType.CHEST, BlockPos.ORIGIN, Blocks.CHEST.getDefaultState());
        this.name = name;
    }

    public void setSlotLocked(int slot, boolean locked) {
        if (slot < 0 || slot >= slotLockState.length)
            return;
        slotLockState[slot] = locked;
    }

    public boolean isSlotLocked(int slot) {
        if (slot < 0 || slot >= slotLockState.length)
            return false;
        return slotLockState[slot];
    }

    public void forceSetStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= inventory.size())
            return;

        getItems().set(slot, stack);
        if (stack.getCount() > stack.getMaxCount()) {
            stack.setCount(stack.getMaxCount());
        }
    }

    public void clearClickCallbacks() {
        clickCallbacks.clear();
    }

    public void setClickCallback(int slot, ChestGUIClickCallback callback) {
        clickCallbacks.put(slot, callback);
    }

    //From the ImplementedInventory Interface

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (clickCallbacks.containsKey(slot)) {
            boolean res = clickCallbacks.get(slot).execute(ChestGUIClickType.CLICK);
            if (!res)
                return;
        }

        // Don't allow placing in locked slots
        if (!slotLockState[slot]) {
            getItems().set(slot, stack);
            if (stack.getCount() > stack.getMaxCount()) {
                stack.setCount(stack.getMaxCount());
            }
        }
    }

    @Override
    public ItemStack removeStack(int slot, int count) {
        if (clickCallbacks.containsKey(slot)) {
            boolean res = clickCallbacks.get(slot).execute(ChestGUIClickType.CLICK);
            if (!res)
                return ItemStack.EMPTY;
        }

        // Don't allow removing from in inventory
        if (slotLockState[slot]) {
            return ItemStack.EMPTY;
        }

        ItemStack result = Inventories.splitStack(getItems(), slot, count);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (clickCallbacks.containsKey(slot)) {
            boolean res = clickCallbacks.get(slot).execute(ChestGUIClickType.CLICK);
            if (!res)
                return ItemStack.EMPTY;
        }

        // Don't allow removing from in inventory
        if (slotLockState[slot]) {
            return ItemStack.EMPTY;
        }
        return Inventories.removeStack(getItems(), slot);
    }

    public boolean allowQuickMove(int slot) {
        if (clickCallbacks.containsKey(slot)) {
            boolean res = clickCallbacks.get(slot).execute(ChestGUIClickType.SHIFT_CLICK);
            if (!res)
                return false;
        }

        return !isSlotLocked(slot);
    }

    //These Methods are from the NamedScreenHandlerFactory Interface
    //createMenu creates the ScreenHandler itself
    //getDisplayName will Provide its name which is normally shown at the top

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        //We provide *this* to the screenHandler as our class Implements Inventory
        //Only the Server has the Inventory at the start, this will be synced to the client in the ScreenHandler
        return new ChestGUIScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public Text getDisplayName() {
        return Text.literal(name);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup wrapper) {
        super.readNbt(nbt, wrapper);
        Inventories.readNbt(nbt, this.inventory, wrapper);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup wrapper) {
        super.writeNbt(nbt, wrapper);
        Inventories.writeNbt(nbt, this.inventory, wrapper);
    }
}
