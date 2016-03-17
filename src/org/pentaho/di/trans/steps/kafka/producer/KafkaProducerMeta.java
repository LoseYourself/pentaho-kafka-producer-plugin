package org.pentaho.di.trans.steps.kafka.producer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import kafka.producer.ProducerConfig;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

public class KafkaProducerMeta extends BaseStepMeta implements StepMetaInterface {
	
	private static Class<?> PKG = KafkaProducerMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	public static final String[] KAFKA_PROPERTIES_NAMES = new String[] { "metadata.broker.list",
			"request.required.acks", "producer.type", "serializer.class", "request.timeout.ms", "key.serializer.class",
			"partitioner.class", "compression.codec", "compressed.topics", "message.send.max.retries",
			"retry.backoff.ms", "topic.metadata.refresh.interval.ms", "queue.buffering.max.ms",
			"queue.buffering.max.messages", "queue.enqueue.timeout.ms", "batch.num.messages", "send.buffer.bytes",
			"client.id" };
	
	public static final Map<String, String> KAFKA_PROPERTIES_DEFAULTS = new HashMap<String, String>();
	
	static {
		KAFKA_PROPERTIES_DEFAULTS.put("metadata.broker.list", "localhost:9092");
		KAFKA_PROPERTIES_DEFAULTS.put("request.required.acks", "1");
		KAFKA_PROPERTIES_DEFAULTS.put("producer.type", "sync");
		KAFKA_PROPERTIES_DEFAULTS.put("serializer.class", "kafka.serializer.DefaultEncoder");
	}

	private Properties kafkaProperties = new Properties();
	private String topic;
	private String messageField;
	private String keyField;

	public Properties getKafkaProperties() {
		return kafkaProperties;
	}

	/**
	 * @return Kafka topic name
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * @param topic
	 *            Kafka topic name
	 */
	public void setTopic(String topic) {
		this.topic = topic;
	}

	/**
	 * @return Target key field name in Kettle stream
	 */
	public String getKeyField() {
		return keyField;
	}

	/**
	 * @param field
	 *            Target key field name in Kettle stream
	 */
	public void setKeyField(String field) {
		this.keyField = field;
	}

	/**
	 * @return Target message field name in Kettle stream
	 */
	public String getMessageField() {
		return messageField;
	}

	/**
	 * @param field
	 *            Target message field name in Kettle stream
	 */
	public void setMessageField(String field) {
		this.messageField = field;
	}

	public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta,
			RowMetaInterface prev, String input[], String output[], RowMetaInterface info) {

		if (topic == null || Const.isEmpty(topic)) {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "KafkaProducerMeta.Check.InvalidTopic"), stepMeta));
		}
		if (messageField == null || Const.isEmpty(messageField)) {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "KafkaProducerMeta.Check.InvalidMessageField"), stepMeta));
		}
		try {
			new ProducerConfig(kafkaProperties);
		} catch (IllegalArgumentException e) {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, e.getMessage(), stepMeta));
		}
	}

	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
			Trans trans) {
		return new KafkaProducer(stepMeta, stepDataInterface, cnr, transMeta, trans);
	}

	public StepDataInterface getStepData() {
		return new KafkaProducerData();
	}

	public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters)
			throws KettleXMLException {

		try {
			topic = XMLHandler.getTagValue(stepnode, "TOPIC");
			messageField = XMLHandler.getTagValue(stepnode, "FIELD");
			keyField = XMLHandler.getTagValue(stepnode, "KEYFIELD");
			Node kafkaNode = XMLHandler.getSubNode(stepnode, "KAFKA");
			for (String name : KAFKA_PROPERTIES_NAMES) {
				String value = XMLHandler.getTagValue(kafkaNode, name);
				if (value != null) {
					kafkaProperties.put(name, value);
				}
			}
		} catch (Exception e) {
			throw new KettleXMLException(BaseMessages.getString(PKG, "KafkaProducerMeta.Exception.loadXml"), e);
		}
	}

	public String getXML() throws KettleException {
		StringBuilder retval = new StringBuilder();
		if (topic != null) {
			retval.append("    ").append(XMLHandler.addTagValue("TOPIC", topic));
		}
		if (messageField != null) {
			retval.append("    ").append(XMLHandler.addTagValue("FIELD", messageField));
		}
		if (keyField != null) {
			retval.append("    ").append(XMLHandler.addTagValue("KEYFIELD", keyField));
		}
		retval.append("    ").append(XMLHandler.openTag("KAFKA")).append(Const.CR);
		for (String name : KAFKA_PROPERTIES_NAMES) {
			String value = kafkaProperties.getProperty(name);
			if (value != null) {
				retval.append("      " + XMLHandler.addTagValue(name, value));
			}
		}
		retval.append("    ").append(XMLHandler.closeTag("KAFKA")).append(Const.CR);
		return retval.toString();
	}

	public void readRep(Repository rep, ObjectId stepId, List<DatabaseMeta> databases, Map<String, Counter> counters)
			throws KettleException {
		try {
			topic = rep.getStepAttributeString(stepId, "TOPIC");
			messageField = rep.getStepAttributeString(stepId, "FIELD");
			keyField = rep.getStepAttributeString(stepId, "KEYFIELD");
			for (String name : KAFKA_PROPERTIES_NAMES) {
				String value = rep.getStepAttributeString(stepId, name);
				if (value != null) {
					kafkaProperties.put(name, value);
				}
			}
		} catch (Exception e) {
			throw new KettleException("KafkaProducerMeta.Exception.loadRep", e);
		}
	}

	public void saveRep(Repository rep, ObjectId transformationId, ObjectId stepId) throws KettleException {
		try {
			if (topic != null) {
				rep.saveStepAttribute(transformationId, stepId, "TOPIC", topic);
			}
			if (messageField != null) {
				rep.saveStepAttribute(transformationId, stepId, "FIELD", messageField);
			}
			if (keyField != null) {
				rep.saveStepAttribute(transformationId, stepId, "KEYFIELD", keyField);
			}
			for (String name : KAFKA_PROPERTIES_NAMES) {
				String value = kafkaProperties.getProperty(name);
				if (value != null) {
					rep.saveStepAttribute(transformationId, stepId, name, value);
				}
			}
		} catch (Exception e) {
			throw new KettleException("KafkaProducerMeta.Exception.saveRep", e);
		}
	}

	public void setDefault() {
	}
}

