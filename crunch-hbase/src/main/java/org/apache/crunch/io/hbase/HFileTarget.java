/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.crunch.io.hbase;

import com.google.common.base.Preconditions;
import org.apache.commons.codec.binary.Hex;
import org.apache.crunch.io.SequentialFileNamingScheme;
import org.apache.crunch.io.impl.FileTargetImpl;
import org.apache.crunch.types.Converter;
import org.apache.crunch.types.PTableType;
import org.apache.crunch.types.PType;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.KeyValueSerialization;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapreduce.Job;

public class HFileTarget extends FileTargetImpl {

  private static final HColumnDescriptor DEFAULT_COLUMN_DESCRIPTOR = new HColumnDescriptor();

  public HFileTarget(String path) {
    this(new Path(path));
  }

  public HFileTarget(Path path) {
    this(path, DEFAULT_COLUMN_DESCRIPTOR);
  }

  public HFileTarget(Path path, HColumnDescriptor hcol) {
    super(path, HFileOutputFormatForCrunch.class, SequentialFileNamingScheme.getInstance());
    Preconditions.checkNotNull(hcol);
    outputConf(HFileOutputFormatForCrunch.HCOLUMN_DESCRIPTOR_KEY,
        Hex.encodeHexString(WritableUtils.toByteArray(hcol)));
  }

  @Override
  public void configureForMapReduce(Job job, PType<?> ptype, Path outputPath, String name) {
    Configuration conf = job.getConfiguration();
    HBaseConfiguration.addHbaseResources(conf);
    conf.setStrings("io.serializations", conf.get("io.serializations"),
        KeyValueSerialization.class.getName());
    super.configureForMapReduce(job, ptype, outputPath, name);
  }

  @Override
  public Converter<?, ?, ?, ?> getConverter(PType<?> ptype) {
    PType<?> valueType = ptype;
    if (ptype instanceof PTableType) {
      valueType = ((PTableType) ptype).getValueType();
    }
    if (!KeyValue.class.equals(valueType.getTypeClass())) {
      throw new IllegalArgumentException("HFileTarget only supports KeyValue outputs");
    }
    if (ptype instanceof PTableType) {
      return new HBasePairConverter<ImmutableBytesWritable, KeyValue>(ImmutableBytesWritable.class, KeyValue.class);
    }
    return new HBaseValueConverter<KeyValue>(KeyValue.class);
  }

  @Override
  public String toString() {
    return "HFile(" + path + ")";
  }
}
