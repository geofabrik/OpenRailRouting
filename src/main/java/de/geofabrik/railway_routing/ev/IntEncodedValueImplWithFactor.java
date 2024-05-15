package de.geofabrik.railway_routing.ev;

import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.IntEncodedValueImpl;

/**
 * Variant of IntEncodedValueImpl which divides values by a given factor like DecimalEncodedValue does.
 * 
 * @param name                   the key to identify this EncodedValue
 * @param bits                   the bits that should be reserved for storing the value. This determines the
 *                               maximum value.
 * @param factor                 the precision factor, i.e. store = (int) Math.round(value / factor)
 * @param minStorableValue       the minimum value. Use e.g. 0 if no negative values are needed.
 * @param negateReverseDirection true if the reverse direction should be always negative of the forward direction.
 *                               This is used to reduce space and store the value only once. If this option is used
 *                               you cannot use storeTwoDirections or a minValue different to 0.
 * @param storeTwoDirections     true if forward and backward direction of the edge should get two independent values.
 */
public class IntEncodedValueImplWithFactor extends IntEncodedValueImpl {
    final double factor;

    public IntEncodedValueImplWithFactor(String name, int bits, double factor, boolean storeTwoDirections) {
        super(name, bits, storeTwoDirections);
        this.factor = factor;
    }

    @Override
    public void setInt(boolean reverse, int edgeId, EdgeIntAccess edgeIntAccess, int value) {
        int v = (int) Math.round(value / factor);
        super.setInt(reverse, edgeId, edgeIntAccess, v);
    }

    @Override
    public int getInt(boolean reverse, int edgeId, EdgeIntAccess edgeIntAccess) {
        return (int) (super.getInt(reverse, edgeId, edgeIntAccess) * factor);
    }

    @Override
    public int getMaxStorableInt() {
        return (int) (super.getMaxStorableInt() * factor);
    }

    @Override
    public int getMinStorableInt() {
        return (int) (super.getMinStorableInt() * factor);
    }

    @Override
    public int getMaxOrMaxStorableInt() {
        return (int) (super.getMaxOrMaxStorableInt() * factor);
    }
}
