package org.jumpmind.symmetric.is.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.exception.IoException;
import org.jumpmind.symmetric.csv.CsvReader;
import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(
        typeName = DelimitedParser.TYPE,
        category = ComponentCategory.PROCESSOR,
        iconImage = "delimitedformatter.png",
        inputMessage = MessageType.NONE,
        outgoingMessage = MessageType.ENTITY)
public class DelimitedParser extends AbstractComponent {

    public static final String TYPE = "Parse Delimited";

    @SettingDefinition(
            order = 10,
            required = true,
            type = Type.STRING,
            label = "Delimiter",
            defaultValue = ",")
    public final static String SETTING_DELIMITER = "delimiter";

    @SettingDefinition(
            order = 20,
            type = Type.STRING,
            label = "Quote Character",
            defaultValue = "\"")
    public final static String SETTING_QUOTE_CHARACTER = "quote.character";

    @SettingDefinition(
            order = 30,
            type = Type.STRING,
            label = "Encoding",
            defaultValue = "UTF-8")
    public final static String SETTING_ENCODING = "encoding";

    public final static String DELIMITED_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION = "delimited.formatter.attribute.format.function";

    public final static String DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL = "delimited.formatter.attribute.ordinal";

    String delimiter = ",";

    String quoteCharacter = "\"";

    String encoding = "UTF-8";

    List<AttributeFormat> attributes = new ArrayList<AttributeFormat>();

    @Override
    public void start(String executionId, IExecutionTracker executionTracker) {
        super.start(executionId, executionTracker);
        delimiter = flowStep.getComponent().get(SETTING_DELIMITER, delimiter);
        quoteCharacter = flowStep.getComponent().get(SETTING_QUOTE_CHARACTER, quoteCharacter);
        encoding = flowStep.getComponent().get(SETTING_ENCODING, encoding);
        convertAttributeSettingsToAttributeFormat();
        if (flowStep.getComponent().getOutputModel() == null) {
            throw new IllegalStateException(
                    "This component requires an output model.  Please select one.");
        }

    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        componentStatistics.incrementInboundMessages();

        ArrayList<String> inputRows = inputMessage.getPayload();

        ArrayList<EntityData> outputPayload = new ArrayList<EntityData>();
        Message outputMessage = inputMessage.copy(flowStep.getId(), outputPayload);

        try {
            // TODO support headers
            for (String inputRow : inputRows) {
                EntityData data = processInputRow(inputRow);
                outputPayload.add(data);
            }
        } catch (IOException e) {
            throw new IoException(e);
        }

        executionTracker.log(executionId, LogLevel.INFO, this, outputPayload.toString());
        componentStatistics.incrementOutboundMessages();
        outputMessage.getHeader()
                .setSequenceNumber(componentStatistics.getNumberOutboundMessages());
        outputMessage.getHeader().setLastMessage(inputMessage.getHeader().isLastMessage());
        messageTarget.put(outputMessage);
    }

    private EntityData processInputRow(String inputRow) throws IOException {

        CsvReader csvReader = new CsvReader(new ByteArrayInputStream(inputRow.getBytes()),
                Charset.forName(encoding));
        csvReader.setDelimiter(delimiter.charAt(0));
        if (isNotBlank(quoteCharacter)) {
            csvReader.setTextQualifier(quoteCharacter.charAt(0));
            csvReader.setUseTextQualifier(true);
        }
        if (csvReader.readRecord()) {
            EntityData data = new EntityData();
            if (attributes.size() > 0) {
                for (AttributeFormat attribute : attributes) {
                    Object value = csvReader.get(attribute.getOrdinal());
                    if (isNotBlank(attribute.getFormatFunction())) {
                        value = ModelAttributeScriptHelper.eval(attribute.getAttribute(), value,
                                attribute.getEntity(), data, attribute.getFormatFunction());
                    }

                    data.put(attribute.getAttributeId(), value);
                }
            } else {
                Model model = flowStep.getComponent().getOutputModel();
                List<ModelEntity> entities = model.getModelEntities();
                int index = 0;
                for (ModelEntity modelEntity : entities) {
                    List<ModelAttribute> attributes = modelEntity.getModelAttributes();
                    for (ModelAttribute modelAttribute : attributes) {
                        data.put(modelAttribute.getId(), csvReader.get(index));
                    }
                }
            }

            return data;
        }
        return null;

    }

    private void convertAttributeSettingsToAttributeFormat() {
        List<ComponentAttributeSetting> attributeSettings = flowStep.getComponent()
                .getAttributeSettings();
        Map<String, AttributeFormat> formats = new HashMap<String, DelimitedParser.AttributeFormat>();
        for (ComponentAttributeSetting attributeSetting : attributeSettings) {
            AttributeFormat format = formats.get(attributeSetting.getAttributeId());
            if (format == null) {
                Model inputModel = flowStep.getComponent().getInputModel();
                ModelAttribute attribute = inputModel.getAttributeById(attributeSetting
                        .getAttributeId());
                ModelEntity entity = inputModel.getEntityById(attribute.getEntityId());
                format = new AttributeFormat(attributeSetting.getAttributeId(), entity, attribute);
                formats.put(attributeSetting.getAttributeId(), format);
            }
            if (attributeSetting.getName().equalsIgnoreCase(DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL)) {
                format.setOrdinal(Integer.parseInt(attributeSetting.getValue()));
            } else if (attributeSetting.getName().equalsIgnoreCase(
                    DELIMITED_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION)) {
                format.setFormatFunction(attributeSetting.getValue());
            }
        }

        attributes.addAll(formats.values());
        Collections.sort(attributes, new Comparator<AttributeFormat>() {
            @Override
            public int compare(AttributeFormat ordinal1, AttributeFormat ordinal2) {
                return ordinal1.getOrdinal() - ordinal2.getOrdinal();
            }
        });

    }

    private class AttributeFormat {

        public AttributeFormat(String attributeId, ModelEntity entity, ModelAttribute attribute) {
            this.attributeId = attributeId;
        }

        ModelEntity entity;

        ModelAttribute attribute;

        String attributeId;

        int ordinal;

        String formatFunction;

        public String getAttributeId() {
            return attributeId;
        }

        public int getOrdinal() {
            return ordinal;
        }

        public void setOrdinal(int ordinal) {
            this.ordinal = ordinal;
        }

        public String getFormatFunction() {
            return formatFunction;
        }

        public void setFormatFunction(String formatFunction) {
            this.formatFunction = formatFunction;
        }

        public ModelAttribute getAttribute() {
            return attribute;
        }

        public ModelEntity getEntity() {
            return entity;
        }
    }

}
