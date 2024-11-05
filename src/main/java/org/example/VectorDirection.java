package org.example;

import ucar.ma2.DataType;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.filter.Enhancement;
import ucar.nc2.filter.EnhancementProvider;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;


public class VectorDirection extends Vectorize {

    private static Logger logger = LogManager.getLogger(VectorDirection.class);

    public static final String ATTRIBUTE_NAME = "vectorize_dir";

    public VectorDirection(Variable var) {
        super(var);
    }

    ReentrantLock lock = new ReentrantLock();

    @Override
    public double convert(double num) {
        lock.lock();
        try {
            double u_val = uVar.read(indexToCoords((int)num), this.n_dimensional_array).getDouble(0);
            double v_val = vVar.read(indexToCoords((int)num), this.n_dimensional_array).getDouble(0);

            // atan2(0, 0) is undefined, so just return 0
            if (Math.sqrt(u_val*u_val + v_val*v_val) == 0.0f) {
                return 0.0f;
            }

            // return values in the [0, 360) range
            return ((Math.toDegrees(Math.atan2(u_val, v_val)) + 360.0f + this.convention_offset) % 360.0f);

        } catch (Exception ex) {
            logger.error(ex);
            return Double.NaN;
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected String getAttributeName() {
        return ATTRIBUTE_NAME;
    }

    public static class Provider implements EnhancementProvider {

        @Override
        public String getAttributeName() {
            return ATTRIBUTE_NAME;
        }

        @Override
        public boolean appliesTo(Set<NetcdfDataset.Enhance> enhance, DataType dt) {
            return dt.isNumeric();
        }

        @Override
        public Enhancement create(VariableDS var) {
            return new VectorDirection(var);
        }
    }
}
