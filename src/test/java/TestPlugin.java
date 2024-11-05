import org.example.VectorDirection;
import org.example.VectorMagnitude;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;


public class TestPlugin {
    @TempDir
    static Path tempDir;

    // data lists
    private final static float[] uData = new float[]{ Float.NaN, 1, 0, 1, 1, 111.1f };
    private final static float[] vData = new float[]{ 1, Float.NaN, 1, 0, 1, 222.2f };
    private final static float[] expectedMags =
            new float[]{ Float.NaN, Float.NaN, 1, 1, (float)Math.sqrt(2), (float)Math.sqrt(uData[5]*uData[5] + vData[5]*vData[5])};

    // direction with to convention: uvar/vvar/to
    private final static String convention = "to";
    private final static float[] expectedDirs =
            new float[]{ Float.NaN, Float.NaN, 0, 90.0f, 45.0f, (float)(((Math.toDegrees(Math.atan2(uData[5], vData[5])) + 360.0f) % 360.0))};

    // direction with from convention: uvar/vvar/from
    // private final static String convention = "from";
    // private final static float[] expectedDirs =
    //         new float[]{ Float.NaN, Float.NaN, 180.0f, 270.0f, 225.0f, (float)(((Math.toDegrees(Math.atan2(uData[5], vData[5])) + 360.0f + 180.0f) % 360.0))};

    // variable names
    private final static String uVar = "u_var";
    private final static String vVar = "v_var";
    private final static String magVar = "mag";
    private final static String dirVar = "dir";

    private final static String uVar2D = "u_var2D";
    private final static String vVar2D = "v_var2D";
    private final static String magVar2D = "mag2D";
    private final static String dirVar2D = "dir2D";

    private final static String uVar3D = "u_var3D";
    private final static String vVar3D = "v_var3D";
    private final static String magVar3D = "mag3D";
    private final static String dirVar3D = "dir3D";

    private final static String magVarMismatch = "magMismatch";
    private final static String dirVarMismatch = "dirMismatch";

    private static NetcdfDataset ds;
    private static final double tol = 0.0001;

    @BeforeAll
    public static void setUpTests() throws IOException, InvalidRangeException {
        Path filePath = Files.createFile(tempDir.resolve("testFile.nc"));
        NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf3(filePath.toString());

        // add dimensions to root
        List<Dimension> dims1D = new ArrayList<>();
        List<Dimension> dims2D = new ArrayList<>();
        List<Dimension> dims3D = new ArrayList<>();
        Dimension dim0 = Dimension.builder().setName("dim0").setLength(uData.length).build();
        builder.addDimension(dim0);
        dims1D.add(dim0);
        dims2D.add(dim0);
        dims3D.add(dim0);
        Dimension dim1 = Dimension.builder().setName("dim1").setLength(uData.length).build();
        builder.addDimension(dim1);
        dims2D.add(dim1);
        dims3D.add(dim1);
        Dimension dim2 = Dimension.builder().setName("dim2").setLength(uData.length).build();
        builder.addDimension(dim2);
        dims3D.add(dim2);

        // add 1D variables
        builder.addVariable(uVar, DataType.FLOAT, dims1D);
        builder.addVariable(vVar, DataType.FLOAT, dims1D);
        builder.addVariable(magVar, DataType.FLOAT, dims1D)
                .addAttribute(new Attribute(VectorMagnitude.ATTRIBUTE_NAME, uVar + "/" + vVar + "/" + convention));
        builder.addVariable(dirVar, DataType.FLOAT, dims1D)
                .addAttribute(new Attribute(VectorDirection.ATTRIBUTE_NAME, uVar + "/" + vVar + "/" + convention));

        // add 2D variables
        builder.addVariable(uVar2D, DataType.FLOAT, dims2D);
        builder.addVariable(vVar2D, DataType.FLOAT, dims2D);
        builder.addVariable(magVar2D, DataType.FLOAT, dims2D)
                .addAttribute(new Attribute(VectorMagnitude.ATTRIBUTE_NAME, uVar2D + "/" + vVar2D + "/" + convention));
        builder.addVariable(dirVar2D, DataType.FLOAT, dims2D)
                .addAttribute(new Attribute(VectorDirection.ATTRIBUTE_NAME, uVar2D + "/" + vVar2D + "/" + convention));

        // add 3D variables
        builder.addVariable(uVar3D, DataType.FLOAT, dims3D);
        builder.addVariable(vVar3D, DataType.FLOAT, dims3D);
        builder.addVariable(magVar3D, DataType.FLOAT, dims3D)
                .addAttribute(new Attribute(VectorMagnitude.ATTRIBUTE_NAME, uVar3D + "/" + vVar3D + "/" + convention));
        builder.addVariable(dirVar3D, DataType.FLOAT, dims3D)
                .addAttribute(new Attribute(VectorDirection.ATTRIBUTE_NAME, uVar3D + "/" + vVar3D + "/" + convention));

        // add vars with mismatched dimensions
        builder.addVariable(magVarMismatch, DataType.FLOAT, "dim0")
                .addAttribute(new Attribute(VectorMagnitude.ATTRIBUTE_NAME, uVar2D + "/" + vVar2D + "/" + convention));
        builder.addVariable(dirVarMismatch, DataType.FLOAT, "dim1")
                .addAttribute(new Attribute(VectorDirection.ATTRIBUTE_NAME, uVar2D + "/" + vVar2D + "/" + convention));

        // write data
        NetcdfFormatWriter writer = builder.build();

        int dataLen = uData.length;

        // 1D
        Array u_array = Array.factory(DataType.FLOAT, new int[]{dataLen}, uData);
        writer.write(writer.findVariable(uVar), u_array);
        Array v_array = Array.factory(DataType.FLOAT, new int[]{dataLen}, vData);
        writer.write(writer.findVariable(vVar), v_array);
        Array indices = Array.makeArray(DataType.FLOAT, dataLen, 0, 1);
        writer.write(writer.findVariable(magVar), indices);
        writer.write(writer.findVariable(dirVar), indices);

        // 2D
        u_array = Array.factory(DataType.FLOAT, new int[]{dataLen, dataLen}, addDim(uData, dataLen));
        writer.write(writer.findVariable(uVar2D), u_array);
        v_array = Array.factory(DataType.FLOAT, new int[]{dataLen, dataLen}, addDim(vData, dataLen));
        writer.write(writer.findVariable(vVar2D), v_array);
        indices = Array.makeArray(DataType.FLOAT, dataLen*dataLen, 0, 1)
                .reshape(new int[]{dataLen, dataLen});
        writer.write(writer.findVariable(magVar2D), indices);
        writer.write(writer.findVariable(dirVar2D), indices);

        // 3D
        u_array = Array.factory(DataType.FLOAT, new int[]{dataLen, dataLen, dataLen}, addDim(addDim(uData, dataLen), dataLen));
        writer.write(writer.findVariable(uVar3D), u_array);
        v_array = Array.factory(DataType.FLOAT, new int[]{dataLen, dataLen, dataLen}, addDim(addDim(vData, dataLen), dataLen));
        writer.write(writer.findVariable(vVar3D), v_array);
        indices = Array.makeArray(DataType.FLOAT, dataLen*dataLen*dataLen, 0, 1)
                .reshape(new int[]{dataLen, dataLen, dataLen});
        writer.write(writer.findVariable(magVar3D), indices);
        writer.write(writer.findVariable(dirVar3D), indices);

        // mismatched
        indices = Array.makeArray(DataType.FLOAT, dataLen, 0, 1);
        writer.write(writer.findVariable(magVarMismatch), indices);
        writer.write(writer.findVariable(dirVarMismatch), indices);

        // finish
        writer.close();
        ds = NetcdfDatasets.openDataset(filePath.toString());
    }

    @AfterAll
    public static void breakDownTests() throws IOException {
        ds.close();
    }

    @Test
    public void testVectorize1D() throws IOException {
        testReadVar(magVar, expectedMags);
        testReadVar(dirVar, expectedDirs);
    }

    @Test
    public void testVectorize2D() throws IOException{
        int n = uData.length;
        testReadVar(magVar2D, addDim(expectedMags, n));
        testReadVar(dirVar2D, addDim(expectedDirs, n));
    }

    @Test
    public void testVectorize3D() throws IOException {
        int n = uData.length;
        testReadVar(magVar3D, addDim(addDim(expectedMags, n), n));
        testReadVar(dirVar3D, addDim(addDim(expectedDirs, n), n));
    }

    @Test
    public void testMismatchedDimension() throws IOException {
        float[] expected = new float[uData.length];
        for (int i = 0; i < uData.length; i++) {
            expected[i] = i;
        }
        testReadVar(magVarMismatch, expected);
        testReadVar(dirVarMismatch, expected);
    }

    private void testReadVar(String varName, float[] expected) throws IOException {
        float[] actual = (float[])ds.findVariable(varName).read().get1DJavaArray(DataType.FLOAT);

        for (int i = 0; i < expected.length; i++) {
            float ex = expected[i];
            float act = actual[i];
            if (Double.isNaN(ex) && Double.isNaN(act)) { continue; }
            if (Math.abs(ex - act) > tol) {
                fail("%s differs at index %d - expected: %f, actual: %f".formatted(varName, i, ex, act));
            }
        }
    }

    private static float[] addDim(float[] in, int dimSize) {
        int n = in.length;
        float[] out = new float[n*dimSize];
        for (int i = 0; i < dimSize; i++) {
            for (int j = 0; j < n; j++) {
                out[i*j] = in[i];
            }
        }
        return out;
    }
}
