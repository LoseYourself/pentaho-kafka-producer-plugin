package org.pentaho.di.trans.steps.kafka.producer;

import kafka.javaapi.producer.Producer;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

public class KafkaProducerData extends BaseStepData implements StepDataInterface {

	Producer<Object, Object> producer;
	RowMetaInterface outputRowMeta;
	int messageFieldNr;
	int keyFieldNr;
	boolean messageIsString;
	boolean keyIsString;
	ValueMetaInterface messageFieldMeta;
	ValueMetaInterface keyFieldMeta;
}
