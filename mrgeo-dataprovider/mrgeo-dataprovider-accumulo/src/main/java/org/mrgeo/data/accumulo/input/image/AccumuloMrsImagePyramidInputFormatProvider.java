/*
 * Copyright 2009-2014 DigitalGlobe, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.mrgeo.data.accumulo.input.image;

import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.iterators.user.RegExFilter;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.mrgeo.image.MrsImagePyramid;
import org.mrgeo.image.MrsImagePyramidMetadata;
import org.mrgeo.data.DataProviderException;
import org.mrgeo.data.accumulo.image.AccumuloMrsImagePyramidInputFormat;
import org.mrgeo.data.accumulo.utils.AccumuloConnector;
import org.mrgeo.data.accumulo.utils.MrGeoAccumuloConstants;
import org.mrgeo.data.image.MrsImageInputFormatProvider;
import org.mrgeo.data.raster.RasterWritable;
import org.mrgeo.data.tile.TileIdWritable;
import org.mrgeo.data.tile.TiledInputFormatContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public class AccumuloMrsImagePyramidInputFormatProvider extends MrsImageInputFormatProvider
{

  private static final Logger log = LoggerFactory.getLogger(AccumuloMrsImagePyramidInputFormatProvider.class);

  //private ArrayList<Integer> zoomLevelsInPyramid;

  private String table;
  private Authorizations auths;
  private Properties props;
  
  public AccumuloMrsImagePyramidInputFormatProvider(TiledInputFormatContext context)
  {
    super(context);
    this.table = context.getFirstInput();
  } // end constructor
  
  public AccumuloMrsImagePyramidInputFormatProvider(Properties props, TiledInputFormatContext context)
  {
    super(context);
    this.table = context.getFirstInput();
    this.props = new Properties();
    this.props.putAll(props);
  } // end constructor
  
  
  @Override
  public InputFormat<TileIdWritable, RasterWritable> getInputFormat(String input)
  {

    table = input;
    if(table.startsWith(MrGeoAccumuloConstants.MRGEO_ACC_PREFIX)){
      table = table.replaceFirst(MrGeoAccumuloConstants.MRGEO_ACC_PREFIX, "");
    }
    
//    if(context.getBounds() == null){
//      //return new AccumuloMrsImagePyramidInputFormat(input, context.getZoomLevel());
//      return null;
//    } else {
      return new AccumuloMrsImagePyramidInputFormat();
//    }
    
//    if(context.getBounds() == null){
//      log.debug("instantiating input format");
//      
//      //return new AccumuloMrsImagePyramidInputFormat(input, context.getZoomLevel());
//      return null;
//    } //else {
//      // don't know what the AllTilesSingle means
//      //return new AccumuloMrsImagePyramidAllTilesSingleInputFormat();
//    //}
//    
//    return null;
    
  } // end getInputFormat
  
  @Override
  public void setupJob(Job job,
      final Properties providerProperties) throws DataProviderException
  {
    super.setupJob(job, providerProperties);

    //zoomLevelsInPyramid = new ArrayList<Integer>();

    // set the needed information
    if(props == null){
      props = new Properties();
      props.putAll(AccumuloConnector.getAccumuloProperties());
    }
    if(props.size() == 0){
      throw new RuntimeException("No configuration for Accumulo!");
    }
    
    for(String k : MrGeoAccumuloConstants.MRGEO_ACC_KEYS_CONNECTION){
      job.getConfiguration().set(k, props.getProperty(k));
    }
    for(String k : MrGeoAccumuloConstants.MRGEO_ACC_KEYS_DATA){
      job.getConfiguration().set(k, props.getProperty(k));
    }
    
//    // set the important configuration items
//    job.getConfiguration().set(MrGeoAccumuloConstants.MRGEO_ACC_KEY_INSTANCE,
//        props.getProperty(MrGeoAccumuloConstants.MRGEO_ACC_KEY_INSTANCE));
//    job.getConfiguration().set(MrGeoAccumuloConstants.MRGEO_ACC_KEY_ZOOKEEPERS,
//        props.getProperty(MrGeoAccumuloConstants.MRGEO_ACC_KEY_ZOOKEEPERS));

    if(props.getProperty(MrGeoAccumuloConstants.MRGEO_ACC_KEY_OUTPUT_TABLE) == null){
      job.getConfiguration().set(MrGeoAccumuloConstants.MRGEO_ACC_KEY_OUTPUT_TABLE, this.table);
    } else {
      job.getConfiguration().set(MrGeoAccumuloConstants.MRGEO_ACC_KEY_OUTPUT_TABLE,
          props.getProperty(MrGeoAccumuloConstants.MRGEO_ACC_KEY_OUTPUT_TABLE));
    }
    
    // username and password
//    job.getConfiguration().set(MrGeoAccumuloConstants.MRGEO_ACC_KEY_USER,
//        props.getProperty(MrGeoAccumuloConstants.MRGEO_ACC_KEY_USER));

    // make sure the password is set with Base64Encoding
    String pw = props.getProperty(MrGeoAccumuloConstants.MRGEO_ACC_KEY_PASSWORD);
    String isEnc = props.getProperty(MrGeoAccumuloConstants.MRGEO_ACC_KEY_PWENCODED64, "false");
    String pwDec = pw;

    if(isEnc.equalsIgnoreCase("true")){
      job.getConfiguration().set(MrGeoAccumuloConstants.MRGEO_ACC_KEY_PASSWORD,
          props.getProperty(MrGeoAccumuloConstants.MRGEO_ACC_KEY_PASSWORD));
      
      pwDec = new String(Base64.decodeBase64(pw.getBytes()));
      job.getConfiguration().set(MrGeoAccumuloConstants.MRGEO_ACC_KEY_PASSWORD,
          pwDec);


    } else {
      byte[] p = Base64.encodeBase64(props.getProperty(MrGeoAccumuloConstants.MRGEO_ACC_KEY_PASSWORD).getBytes());
      
      job.getConfiguration().set(MrGeoAccumuloConstants.MRGEO_ACC_KEY_PASSWORD,
          new String(p));
      job.getConfiguration().set(MrGeoAccumuloConstants.MRGEO_ACC_KEY_PWENCODED64,
          new String("true"));
    }

    if(job.getConfiguration().get("protectionLevel") != null){
    	job.getConfiguration().set(MrGeoAccumuloConstants.MRGEO_ACC_KEY_VIZ,
    			job.getConfiguration().get("protectionLevel"));
    }
    
//    if(props.containsKey(MrGeoAccumuloConstants.MRGEO_ACC_KEY_VIZ)){
//      job.getConfiguration().set(MrGeoAccumuloConstants.MRGEO_ACC_KEY_VIZ,
//          props.getProperty(MrGeoAccumuloConstants.MRGEO_ACC_KEY_VIZ));
//    }

    if(props.getProperty(MrGeoAccumuloConstants.MRGEO_ACC_KEY_AUTHS) != null){
      auths = new Authorizations(props.getProperty(MrGeoAccumuloConstants.MRGEO_ACC_KEY_AUTHS).split(","));
    } else {
      auths = new Authorizations();
    }

    String enc = AccumuloConnector.encodeAccumuloProperties(context.getFirstInput());
    job.getConfiguration().set(MrGeoAccumuloConstants.MRGEO_ACC_KEY_ENCODED, enc);
    // get the input table
    for(final String input : context.getInputs()){
      // put encoded string for Accumulo connections 
      
      
      
      
      
      MrsImagePyramid pyramid;
      try{
        pyramid = MrsImagePyramid.open(input, job.getConfiguration());
        final MrsImagePyramidMetadata metadata = pyramid.getMetadata();
        log.debug("In setupJob(), loading pyramid for " + input +
            " pyramid instance is " + pyramid + " metadata instance is " + metadata);

//      ImageMetadata[] im = metadata.getImageMetadata();
        // im[0] - should be null
        //im[0].name; // string that is the zoom level

        //int z = context.getZoomLevel();
        // check to see if the zoom level is in the list of zoom levels
//        if(! zoomLevelsInPyramid.contains(z)){
//          z = metadata.getMaxZoomLevel();
//        }
      } catch(IOException ioe){
        throw new DataProviderException("Failure opening input image pyramid: " + input, ioe);
      }

      
      
    } // end for loop

    //job.setInputFormatClass(AccumuloMrsImagePyramidInputFormat.class);
    
    // check for base64 encoded password
    AccumuloMrsImagePyramidInputFormat.setInputInfo(job.getConfiguration(),
        props.getProperty(MrGeoAccumuloConstants.MRGEO_ACC_KEY_USER),
        pwDec.getBytes(),
        table,
        auths);
    
    AccumuloMrsImagePyramidInputFormat.setZooKeeperInstance(job,
        props.getProperty(MrGeoAccumuloConstants.MRGEO_ACC_KEY_INSTANCE),
        props.getProperty(MrGeoAccumuloConstants.MRGEO_ACC_KEY_ZOOKEEPERS));
    
    // think about scanners - set the zoom level of the job
    IteratorSetting regex = new IteratorSetting(51, "regex", RegExFilter.class);
    RegExFilter.setRegexs(regex, null, Integer.toString(context.getZoomLevel()), null, null, false);
    AccumuloMrsImagePyramidInputFormat.addIterator(job, regex);
    
  } // end setupJob

  @Override
  public void teardown(Job job) throws DataProviderException
  {
  }

} // end AccumuloMrsImagePyramidInputFormatProvider
