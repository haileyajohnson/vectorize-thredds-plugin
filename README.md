# Vectorize: a THREDDS plugin

### About
This plugin allows THREDDS projects to read vector data stored as `u` and `v` components as `magnitude` and `direction` components (useful for showing vectors with WMS).

It leverages netCDF-Java's service provider mechanism for [`Enhancements`](https://docs.unidata.ucar.edu/netcdf-java/5.6/userguide/netcdf_dataset.html#netcdfdatasetenhance) to make two new `Attributes` readable to NetCDF-Java and virtually modify a dataset.
It can be used from the [THREDDS Data Server](https://docs.unidata.ucar.edu/tds/current/userguide/index.html) or directly from the [netCDF-Java library](https://docs.unidata.ucar.edu/netcdf-java/5.6/userguide/index.html).

### Deploying
 * Step 1: jar the plugin with `mvn package`
 * Step 2: put the jar on your netCDF-Java classpath (see [Runtime Loading](https://docs.unidata.ucar.edu/netcdf-java/5.6/userguide/runtime_loading.html)). For a standard TDS instalation, this is the `/usr/local/tomcat/webapps/thredds##X.X/WEB-INF/lib/` folder.
 * Step 3: That's it, it should just work.

#### Requirements
- netCDF-Java 5.6.1+ (or SHAPSHOT releases after 10/25/2024)
    - netCDF-Java 5.6.1+ is packaged with TDS 5.6+ (or SNAPSHOTS from after the same date)
    - TDS 5.6+ requires Java 17 and Tomcat 10

### How to use it
To use `Vectorize`, you will need to do the following:
1) make two new variables in your dataset in the same `Group` as your `u` and `v`: one for vector magnitude and one for vector direction
2) give these variables the same `Dimensions` as your `u` and `v` variables (`u` and `v` must share the same `Dimension` set)
3) add an `Attribute` to your magnitude variable with `name="vectorize_mag"` and `value="{U var name}/{V var name}/{to|from}"`
4) add an `Attribute` to your direction variable with `name="vectorize_dir"` and `value="{U var name}/{V var name}/{to|from}"`

The convention for the direction must be specified using "to" or "from":

* "to": represent the direction the variable is going towards (oceanographic convention)
* "from": represent the direction the variable is coming from (meteorological convention)

These new variables will be read by NetCDF-Java (and the TDS) as the magnitude and directions of the provided `u` and `v` variables.

#### netCDF-Java API
If you're using netCDF-Java directly in your project, you can add the new `Variables` as follows:
~~~java
  NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.openExisting("pathToMyFile");

  // add new variables with attributes
  builder.addVariable(magVar, DataType.FLOAT, "myDim")
          .addAttribute(new Attribute(VectorMagnitude.ATTRIBUTE_NAME, "myUVarName/myVVarName/to"));
  builder.addVariable(dirVar, DataType.FLOAT, "myDim")
          .addAttribute(new Attribute(VectorDirection.ATTRIBUTE_NAME, "myUVarName/myVVarName/to"));

  // write data to new vars
  NetcdfFormatWriter writer = builder.build()
  Array indices = Array.makeArray(DataType.FLOAT, dataLen, 0, 1);
  writer.write(writer.findVariable(magVar), indices);
  writer.write(writer.findVariable(dirVar), indices);
~~~
See [here](https://docs.unidata.ucar.edu/netcdf-java/5.6/userguide/writing_netcdf.html) for more about writing to `NetcdfDataset` objects.

#### NcML
If you're using the plugin in the TDS, you can add virtual variables to your dataset with NcML:
~~~xml
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" location="{myDatasetLocation}">
  ...
  <variable name="magnitude" shape="{ same dims as U and V }" type="float">
    <attribute name="vectorize_mag" value="myUVarName/myVVarName/to" />
    <values start="0" incr="1" />
    <attribute name="long_name" value="magnitude" />
    <attribute name="units" value="m/s" />
  </variable>
  <variable name="direction" shape="{ same dims as U and V}" type="float">
    <attribute name="vectorize_dir" value="myUVarName/myVVarName/to" />
    <values start="0" incr="1" />
    <attribute name="long_name" value="direction" />
    <attribute name="units" value="degrees" />
  </variable>
  ...
</netcdf>
~~~
See [here](https://docs.unidata.ucar.edu/thredds/ncml/2.2/index.html) for more on using NcML.

### Developing and testing
This project uses basic maven to build, test, and package.
- To test: `mvn test`
- To build:`mvn compile`
- To create a jar file: `mvn package`
