package org.kie.kogito.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.kie.kogito.Model;
import org.kie.kogito.examples.demo.Order;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.ProcessInstances;
import org.kie.kogito.process.WorkItem;

import io.quarkus.test.junit.QuarkusTest;


@QuarkusTest
public class OrdersProcessTest {

    @Inject
    @Named("demo.orders")
    Process<? extends Model> orderProcess;
    
    @Inject
    @Named("demo.orderItems")
    Process<? extends Model> orderItemsProcess;
    
    @Test
    public void testOrderProcess() {
        assertNotNull(orderProcess);
        
        Model m = orderProcess.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("approver", "john");
        parameters.put("order", new Order("12345", false, 0.0));
        m.fromMap(parameters);
        
        ProcessInstance<?> processInstance = orderProcess.createInstance(m);
        processInstance.start();
        
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, processInstance.status());
        Model result = (Model)processInstance.variables();
        assertEquals(2, result.toMap().size());
        assertTrue(((Order)result.toMap().get("order")).getTotal() > 0);

        ProcessInstances<? extends Model> orderItemProcesses = orderItemsProcess.instances();
        assertEquals(1, orderItemProcesses.values().size());
        
        ProcessInstance<?> childProcessInstance = orderItemProcesses.values().iterator().next();
        
        List<WorkItem> workItems = childProcessInstance.workItems();
        assertEquals(1, workItems.size());
        
        childProcessInstance.completeWorkItem(workItems.get(0).getId(), null);
        
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, childProcessInstance.status());                
        
        processInstance.abort();
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ABORTED, processInstance.status());
    }
}
