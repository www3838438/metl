/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.runtime.component;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.Folder;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.ExecutionTrackerNoOp;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.utils.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DelimitedFormatterTest {

    private FlowStep delimitedFormatterFlowStep;
    
    @Before
    public void setup() throws Exception {
        delimitedFormatterFlowStep = createDelimitedFormatterFlowStep();
    }

    @After
    public void tearDown() throws Exception {
    }
    
    @Test
    public void testDelimitedFormatterFromSingleContentMsg() throws Exception {
        DelimitedFormatter delimitedFormatter = new DelimitedFormatter();
        delimitedFormatter.setContext(new ComponentContext(null, delimitedFormatterFlowStep, null, new ExecutionTrackerNoOp(), null, null, null,null));
        delimitedFormatter.start();
        Message message = createInboundMessage();        
        SendMessageCallback<ArrayList<EntityData>> msgTarget = new SendMessageCallback<ArrayList<EntityData>>();
        delimitedFormatter.handle(message, msgTarget, true);

        assertEquals(1, msgTarget.getPayloadList().size());
        ArrayList<EntityData> payload = msgTarget.getPayloadList().get(0);
        assertEquals(1,payload.size());
        assertEquals("tt1col2_value|tt1col1_value|tt2col2_value||tt1col3_value|tt2col1_value", payload.get(0));
    }

    private static Message createInboundMessage() {
        
        EntityDataMessage message = new EntityDataMessage("fake step id");
        ArrayList<EntityData> inboundPayload = new ArrayList<EntityData>();
        EntityData entityData = new EntityData();
        entityData.put("tt1col1", "tt1col1_value");
        entityData.put("tt1col2", "tt1col2_value");
        entityData.put("tt1col3", "tt1col3_value");
        entityData.put("tt2col1", "tt2col1_value");
        entityData.put("tt2col2", "tt2col2_value");
//        entityData.put("tt2col3", "tt2col3_value");
        entityData.put("tt2col3", null);
        
        inboundPayload.add(entityData);
        message.setPayload(inboundPayload);
        
        return message;
    }
    
    private static FlowStep createDelimitedFormatterFlowStep() {
        Folder folder = TestUtils.createFolder("Test Folder");
        Flow flow = TestUtils.createFlow("TestFlow", folder);
        Setting[] settingData = createDelimitedFormatterSettings();
        Component component = TestUtils.createComponent(DelimitedFormatter.TYPE, false, null,
                createInputModel(), null, null, createAttributeSettings(), settingData);

        FlowStep formatterFlowStep = new FlowStep();
        formatterFlowStep.setFlowId(flow.getId());
        formatterFlowStep.setComponentId(component.getId());
        formatterFlowStep.setCreateBy("Test");
        formatterFlowStep.setCreateTime(new Date());
        formatterFlowStep.setLastUpdateBy("Test");
        formatterFlowStep.setLastUpdateTime(new Date());
        formatterFlowStep.setComponent(component);
        return formatterFlowStep;
    }
    
    private static Setting[] createDelimitedFormatterSettings() {

        Setting[] settingData = new Setting[2];
        settingData[0] = new Setting(DelimitedFormatter.DELIMITED_FORMATTER_DELIMITER,"|");
        settingData[1] = new Setting(DelimitedFormatter.DELIMITED_FORMATTER_QUOTE_CHARACTER,"");

        return settingData;
    }
    
    private static List<ComponentAttribSetting> createAttributeSettings() { 
        
        List<ComponentAttribSetting> attributeSettings = new ArrayList<ComponentAttribSetting>();
        attributeSettings.add(new ComponentAttribSetting("tt1col2", DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL, "10"));
        attributeSettings.add(new ComponentAttribSetting("tt1col1", DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL, "20"));
        attributeSettings.add(new ComponentAttribSetting("tt2col2", DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL, "30"));
        attributeSettings.add(new ComponentAttribSetting("tt2col3", DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL, "50"));
        attributeSettings.add(new ComponentAttribSetting("tt1col3", DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL, "100"));
        attributeSettings.add(new ComponentAttribSetting("tt2col1", DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL, "120"));
                
        return attributeSettings;
        
    }
    
    private static Model createInputModel() {

        ModelEntity tt1 = new ModelEntity("tt1", "TEST_TABLE_1");
        tt1.addModelAttribute(new ModelAttrib("tt1col1", tt1.getId(), "COL1"));
        tt1.addModelAttribute(new ModelAttrib("tt1col2", tt1.getId(), "COL2"));
        tt1.addModelAttribute(new ModelAttrib("tt1col3", tt1.getId(), "COL3"));

        ModelEntity tt2 = new ModelEntity("tt2", "TEST_TABLE_2");
        tt2.addModelAttribute(new ModelAttrib("tt2col1", tt1.getId(), "COLX"));
        tt2.addModelAttribute(new ModelAttrib("tt2col2", tt1.getId(), "COLY"));
        tt2.addModelAttribute(new ModelAttrib("tt2col3", tt1.getId(), "COLZ"));

        Model model = new Model();
        model.getModelEntities().add(tt1);
        model.getModelEntities().add(tt2);

        return model;
    }

}
