package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.model.GroupProcessorsEntity;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.model.ProcessGroupEntity;
import com.github.hermannpencole.nifi.swagger.client.model.ProcessGroupFlowDTO;
import com.github.hermannpencole.nifi.swagger.client.model.ProcessGroupFlowEntity;
import com.github.hermannpencole.nifi.swagger.client.model.ProcessorDTO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * Class that offer service for nifi processor
 * <p>
 * Created by SFRJ on 01/04/2017.
 */
@Singleton
public class ExtractProcessorService {


    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(ExtractProcessorService.class);

    @Inject
    private ProcessGroupService processGroupService;

    @Inject
    private FlowApi flowapi;

    /**
     *
     * @param branch
     * @param fileConfiguration
     * @throws IOException
     * @throws ApiException
     */
    public void extractByBranch(List<String> branch, String fileConfiguration) throws IOException, ApiException {
        File file = new File(fileConfiguration);

        ProcessGroupFlowEntity componentSearch = processGroupService.changeDirectory(branch)
                .orElseThrow(() -> new ConfigException(("cannot find " + Arrays.toString(branch.toArray()))));

        GroupProcessorsEntity result = extractJsonFromComponent(componentSearch);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        LOG.debug("saving in file {}", fileConfiguration);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            gson.toJson(result, writer);
        } finally {
            LOG.debug("extractByBranch end");
        }
    }

    /**
     * extract from component
     *
     * @param idComponent
     * @return
     * @throws ApiException
     */
    private GroupProcessorsEntity extractJsonFromComponent(ProcessGroupFlowEntity idComponent) throws ApiException {
        GroupProcessorsEntity result = new GroupProcessorsEntity();
        ProcessGroupFlowDTO processGroupFlow = idComponent.getProcessGroupFlow();
        result.setName(processGroupFlow.getBreadcrumb().getBreadcrumb().getName());
        processGroupFlow.getFlow().getProcessors()
                .forEach(processor -> result.getProcessors().add(extractProcessor(processor.getComponent())));
        for (ProcessGroupEntity processGroups : processGroupFlow.getFlow().getProcessGroups()) {
            result.getGroupProcessorsEntity().add(extractJsonFromComponent(flowapi.getFlow(processGroups.getId())));
        }
        if (result.getGroupProcessorsEntity().isEmpty()) {
            result.setGroupProcessorsEntity(null);
        }
        if (result.getProcessors().isEmpty()) {
            result.setProcessors(null);
        }
        return result;
    }

    /**
     * extract processor configuration
     *
     * @param processor
     * @return
     */
    private ProcessorDTO extractProcessor(ProcessorDTO processor) {
        ProcessorDTO result = new ProcessorDTO();
        result.setName(processor.getName());
        result.setConfig(processor.getConfig());
        result.getConfig().setAutoTerminatedRelationships(null);
        result.getConfig().setDescriptors(null);
        result.getConfig().setDefaultConcurrentTasks(null);
        result.getConfig().setDefaultSchedulingPeriod(null);
        result.setRelationships(null);
        result.setStyle(null);
        result.setSupportsBatching(null);
        result.setSupportsEventDriven(null);
        result.setSupportsParallelProcessing(null);
        result.setPersistsState(null);
        result.setRestricted(null);
        result.setValidationErrors(null);
        return result;
    }

}
