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


public class VectorMagnitude extends Vectorize {

    private static Logger logger = LogManager.getLogger(VectorMagnitude.class);

    public static final String ATTRIBUTE_NAME = "vectorize_mag";

    public VectorMagnitude(Variable var) {
        super(var);
    }

    ReentrantLock lock = new ReentrantLock();

    @Override
    public double convert(double num) {
        lock.lock();
        try {
            double u_val = uVar.read(indexToCoords((int)num), this.n_dimensional_array).getDouble(0);
            double v_val = vVar.read(indexToCoords((int)num), this.n_dimensional_array).getDouble(0);
            return Math.sqrt(u_val*u_val + v_val*v_val);
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
            return new VectorMagnitude(var);
        }
    }
}

