package com.raoulvdberge.refinedpipes.network.graph.scanner;

import com.raoulvdberge.refinedpipes.network.NetworkManager;
import com.raoulvdberge.refinedpipes.network.pipe.Destination;
import com.raoulvdberge.refinedpipes.network.pipe.Pipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.*;

public class NetworkGraphScanner {
    private final Set<Pipe> foundPipes = new HashSet<>();
    private final Set<Pipe> newPipes = new HashSet<>();
    private final Set<Pipe> removedPipes = new HashSet<>();
    private final Set<Destination<IItemHandler>> destinations = new HashSet<>();
    private final Set<Pipe> currentPipes;

    private final List<NetworkGraphScannerRequest> allRequests = new ArrayList<>();
    private final Queue<NetworkGraphScannerRequest> requests = new ArrayDeque<>();

    public NetworkGraphScanner(Set<Pipe> currentPipes) {
        this.currentPipes = currentPipes;
        this.removedPipes.addAll(currentPipes);
    }

    public NetworkGraphScannerResult scanAt(World world, BlockPos pos) {
        addRequest(new NetworkGraphScannerRequest(world, pos, null));

        NetworkGraphScannerRequest request;
        while ((request = requests.poll()) != null) {
            singleScanAt(request);
        }

        return new NetworkGraphScannerResult(
            foundPipes,
            newPipes,
            removedPipes,
            destinations, // TODO: both with extractor and none doesn't work, loop
            allRequests
        );
    }

    private void singleScanAt(NetworkGraphScannerRequest request) {
        Pipe pipe = NetworkManager.get(request.getWorld()).getPipe(request.getPos());

        if (pipe != null) {
            if (foundPipes.add(pipe)) {
                if (!currentPipes.contains(pipe)) {
                    newPipes.add(pipe);
                }

                removedPipes.remove(pipe);

                request.setSuccessful(true);

                for (Direction dir : Direction.values()) {
                    addRequest(new NetworkGraphScannerRequest(
                        request.getWorld(),
                        request.getPos().offset(dir),
                        request
                    ));
                }
            }
        } else if (request.getParent() != null) {
            Pipe connectedPipe = NetworkManager.get(request.getWorld()).getPipe(request.getParent().getPos());

            TileEntity tile = request.getWorld().getTileEntity(request.getPos());

            if (tile != null) {
                tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)
                    .ifPresent(itemHandler -> destinations.add(
                        new Destination<>(itemHandler, request.getPos(), connectedPipe)
                    ));
            }
        }
    }

    private void addRequest(NetworkGraphScannerRequest request) {
        requests.add(request);
        allRequests.add(request);
    }
}
