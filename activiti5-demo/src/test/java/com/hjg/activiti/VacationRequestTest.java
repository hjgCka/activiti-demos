package com.hjg.activiti;

import org.activiti.engine.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author hjg
 * @Date 2022-11-21 0:51
 */
public class VacationRequestTest {

    private static final Logger logger = LoggerFactory.getLogger(VacationRequestTest.class);

    private ProcessEngine processEngine;

    @BeforeEach
    public void init() {
        //会寻找classpath上的activiti.cfg.xml文件，基于这个配置文件来构建流程引擎
        processEngine = ProcessEngines.getDefaultProcessEngine();
    }

    /**
     * 部署流程定义，xml文件名称必须要以bpmn20.xml结尾
     */
    @Test
    public void vacationDeploy() {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("vacation/vacation_request.bpmn20.xml")
                //.addClasspathResource("vacation/api.vacationRequest.png")
                .deploy();
        logger.info("deployment id = {}", deployment.getId());

        logger.info("Number of process definitions: " + repositoryService.createProcessDefinitionQuery().count());
    }

    @Test
    public void vacationStart() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("employeeName", "Kermit");
        variables.put("numberOfDays", new Integer(4));
        variables.put("vacationMotivation", "I'm really tired!");

        RuntimeService runtimeService = processEngine.getRuntimeService();
        //这个key是xml文件的 id 属性
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("vacationRequest", variables);
        logger.info("procInstId = {}", processInstance.getId());

        // Verify that we started a new process instance
        logger.info("Number of process instances: " + runtimeService.createProcessInstanceQuery().count());
    }

    @Test
    public void vacationComplete() {
        // Fetch all tasks for the management group
        TaskService taskService = processEngine.getTaskService();
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("management").list();
        for (Task task : tasks) {
            logger.info("Task available: " + task.getName());
        }

        Task task = tasks.get(0);

        Map<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("vacationApproved", "false");
        taskVariables.put("managerMotivation", "We have a tight deadline!");
        taskService.complete(task.getId(), taskVariables);
    }

    /**
     * 挂起或激活流程
     * When a process definition is suspended, new process instance can’t be created (an exception will be thrown).
     *
     * It’s also possible to suspend a process instance. When suspended, the process cannot be continued
     * (e.g. completing a task throws an exception) and no jobs (such as timers) will executed.
     * runtimeService.suspendProcessInstance
     * runtimeService.activateProcessInstanceXXX
     */
    @Test
    public void suspendAndActivateTest() {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        RuntimeService runtimeService = processEngine.getRuntimeService();

        String processDefinitionKey = "vacationRequest";
        repositoryService.suspendProcessDefinitionByKey(processDefinitionKey);
        try {
            runtimeService.startProcessInstanceByKey(processDefinitionKey);
        } catch (ActivitiException e) {
            logger.error("启动流程时异常", e);
        }

        repositoryService.activateProcessDefinitionByKey(processDefinitionKey);
    }

}
