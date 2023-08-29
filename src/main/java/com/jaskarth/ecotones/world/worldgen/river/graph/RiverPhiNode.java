package com.jaskarth.ecotones.world.worldgen.river.graph;

// Node that merges 2 subgraphs
// See also: phi node (compiler theory)
public class RiverPhiNode extends RiverNode {
    @Deprecated
    public RiverNode next;
    public RiverPhiNode(double x, double z, double radius, double depth, double angle) {
        super(x, z, radius, depth, angle);
    }
}
