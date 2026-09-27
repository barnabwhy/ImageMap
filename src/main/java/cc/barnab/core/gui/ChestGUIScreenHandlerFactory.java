package cc.barnab.core.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class ChestGUIScreenHandlerFactory extends BlockEntity implements MenuProvider, ImplementedInventory {
    private final NonNullList<@NotNull ItemStack> inventory = NonNullList.withSize(9 * 6, ItemStack.EMPTY);

    private final boolean[] slotLockState = new boolean[9*6];

    private final HashMap<Integer, ChestGUIClickCallback> clickCallbacks = new HashMap<>();

    private final String name;

    public ChestGUIScreenHandlerFactory(String name) {
        super(BlockEntityTypes.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
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
        if (stack.getCount() > stack.getMaxStackSize()) {
            stack.setCount(stack.getMaxStackSize());
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
    public NonNullList<@NotNull ItemStack> getItems() {
        return inventory;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (clickCallbacks.containsKey(slot)) {
            boolean res = clickCallbacks.get(slot).execute(ChestGUIClickType.CLICK);
            if (!res)
                return;
        }

        // Don't allow placing in locked slots
        if (!slotLockState[slot]) {
            getItems().set(slot, stack);
            if (stack.getCount() > stack.getMaxStackSize()) {
                stack.setCount(stack.getMaxStackSize());
            }
        }
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int count) {
        if (clickCallbacks.containsKey(slot)) {
            boolean res = clickCallbacks.get(slot).execute(ChestGUIClickType.CLICK);
            if (!res)
                return ItemStack.EMPTY;
        }

        // Don't allow removing from in inventory
        if (slotLockState[slot]) {
            return ItemStack.EMPTY;
        }

        ItemStack result = ContainerHelper.removeItem(getItems(), slot, count);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        if (clickCallbacks.containsKey(slot)) {
            boolean res = clickCallbacks.get(slot).execute(ChestGUIClickType.CLICK);
            if (!res)
                return ItemStack.EMPTY;
        }

        // Don't allow removing from in inventory
        if (slotLockState[slot]) {
            return ItemStack.EMPTY;
        }
        return ContainerHelper.takeItem(getItems(), slot);
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
    public AbstractContainerMenu createMenu(int syncId, @NotNull Inventory playerInventory, @NotNull Player player) {
        //We provide *this* to the screenHandler as our class Implements Inventory
        //Only the Server has the Inventory at the start, this will be synced to the client in the ScreenHandler
        return new ChestGUIScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal(name);
    }

//    @Override
//    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup wrapper) {
//        super.readNbt(nbt, wrapper);
//        Inventories.readNbt(nbt, this.inventory, wrapper);
//    }
//
//    @Override
//    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup wrapper) {
//        super.writeNbt(nbt, wrapper);
//        Inventories.writeNbt(nbt, this.inventory, wrapper);
//    }
}
