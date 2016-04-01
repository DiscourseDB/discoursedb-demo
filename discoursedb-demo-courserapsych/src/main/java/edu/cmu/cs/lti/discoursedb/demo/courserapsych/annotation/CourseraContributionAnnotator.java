package edu.cmu.cs.lti.discoursedb.demo.courserapsych.annotation;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import edu.cmu.cs.lti.discoursedb.configuration.BaseConfiguration;
import edu.cmu.cs.lti.discoursedb.core.service.annotation.AnnotationService;
import edu.cmu.cs.lti.discoursedb.core.service.macro.ContributionService;
import edu.cmu.cs.lti.discoursedb.io.coursera.model.CourseraSourceMapping;

/**
 * Adds annotations to contributions based on two provided mapping files that map from post/comment ids to a label.
 * 
 * @author Oliver Ferschke
 */
@Component
@SpringBootApplication
@ComponentScan(	basePackages = { "edu.cmu.cs.lti.discoursedb.configuration", "edu.cmu.cs.lti.discoursedb.demo.courserapsych.annotation" }, 
				useDefaultFilters = false, 
				includeFilters = {@ComponentScan.Filter(
						type = FilterType.ASSIGNABLE_TYPE, 
						value = {CourseraContributionAnnotator.class, BaseConfiguration.class })})
public class CourseraContributionAnnotator implements CommandLineRunner {
	
	@Autowired private ContributionService contribService;
	@Autowired private AnnotationService annoService;
	
	private static String dataSetName;
	private static Map<String,String> postToAnno;
	private static Map<String,String> commentToAnno;	
	
	/**
	 * Launches the SpringBoot application 
	 * 
	 * @param args 
	 */
	public static void main(String[] args) throws IOException{
		Assert.isTrue(args.length==3,"USAGE: SimpleContributionAnnotator <dataSetName> <postToAnnoMapFile> <commentToAnnoMapFile>");
		dataSetName = args[0];
		postToAnno = loadMapping(args[1]);
		commentToAnno = loadMapping(args[2]);
        SpringApplication.run(CourseraContributionAnnotator.class, args);       
	}
	
	private static Map<String,String> loadMapping(String file) throws IOException{
		return FileUtils.readLines(new File(file)).stream()
				.map(line -> line.split(","))
				.collect(Collectors.toMap(a -> a[0], a -> a[1]));
	}
	
	@Override
	@Transactional
	public void run(String... args) throws Exception {		
		
		for(Entry<String,String> curEntry:postToAnno.entrySet()){
			contribService.findOneByDataSource(curEntry.getKey(), CourseraSourceMapping.ID_STR_TO_CONTRIBUTION, dataSetName).ifPresent(contrib ->{
				annoService.addAnnotation(contrib,annoService.createTypedAnnotation(curEntry.getValue()));
			});			
		}
		for(Entry<String,String> curEntry:commentToAnno.entrySet()){
			contribService.findOneByDataSource(curEntry.getKey(), CourseraSourceMapping.ID_STR_TO_CONTRIBUTION_COMMENT, dataSetName).ifPresent(contrib ->{
				annoService.addAnnotation(contrib,annoService.createTypedAnnotation(curEntry.getValue()));
			});			
		}		
	}
}
