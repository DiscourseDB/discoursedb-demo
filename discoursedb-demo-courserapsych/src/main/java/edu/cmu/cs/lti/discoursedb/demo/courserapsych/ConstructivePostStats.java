package edu.cmu.cs.lti.discoursedb.demo.courserapsych;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityNotFoundException;

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
import edu.cmu.cs.lti.discoursedb.core.model.annotation.AnnotationInstance;
import edu.cmu.cs.lti.discoursedb.core.model.macro.Contribution;
import edu.cmu.cs.lti.discoursedb.core.model.macro.Discourse;
import edu.cmu.cs.lti.discoursedb.core.model.user.User;
import edu.cmu.cs.lti.discoursedb.core.service.macro.ContributionService;
import edu.cmu.cs.lti.discoursedb.core.service.macro.DiscourseService;
import edu.cmu.cs.lti.discoursedb.core.service.user.UserService;

/**
 * Calculates contribution count statistics based on contribution annotations.
 * Every contributions annotated with a C1, C2 or I label is considered to be a constructive contribution.
 * All others are not.
 * 
 * The custom.properties file needs to point to a DiscourseDB instance that contains the desired dataset.
 * The name of the discourse that represents the coursea data in that database has to be provided as the first command line parameter.  
 * Output is written to a csv file. The location of that file has to be provided as a second command line parameter.
 * 
 * 
 * @author Oliver Ferschke
 */
@Component
@SpringBootApplication
@ComponentScan(	basePackages = { "edu.cmu.cs.lti.discoursedb.configuration", "edu.cmu.cs.lti.discoursedb.demo.courserapsych" }, 
				useDefaultFilters = false, 
				includeFilters = {@ComponentScan.Filter(
						type = FilterType.ASSIGNABLE_TYPE, 
						value = {ConstructivePostStats.class, BaseConfiguration.class })})
public class ConstructivePostStats implements CommandLineRunner {
	
	@Autowired private ContributionService contribService;
	@Autowired private UserService userService;
	@Autowired private DiscourseService discourseService;

	private static String discourseName;
	private static String outFile;
	private List<String> constructiveLabels = Arrays.asList(new String[]{"C1","C2","I"});
	
	/**
	 * Launches the SpringBoot application 
	 * 
	 * @param args Command line parans: <DiscourseName> <PathOfOutputFile>"
	 */
	public static void main(String[] args) throws IOException{
		Assert.isTrue(args.length==2,"USAGE: SimpleContributionAnnotator <DiscourseName> <Output>");
		discourseName = args[0];
		outFile = args[1];
        SpringApplication.run(ConstructivePostStats.class, args);       
	}
	
	
	@Override
	@Transactional
	public void run(String... args) throws Exception {		
		List<String> outputLines = new ArrayList<>();
		outputLines.add("userid,username,constructiveContributions,allContributions");
		
		Discourse discourse = discourseService.findOne(discourseName).orElseThrow(()->new EntityNotFoundException("Could not find discourse with name "+discourseName));

		for(User curUser:userService.findUsersByDiscourse(discourse)){
			int allContributions = 0;
			int constructiveContributions = 0;
			
			for(Contribution contrib: contribService.findAllByFirstRevisionUser(curUser)){
				allContributions++;
				if(contrib.getAnnotations()!=null){
					boolean constructive = false;
					for(AnnotationInstance anno:contrib.getAnnotations().getAnnotations()){
						if(constructiveLabels.contains(anno.getType().toUpperCase())){
							constructive=true;
						}
					}
					if(constructive){constructiveContributions++;}
				}
			}
			outputLines.add(curUser.getId()+","+curUser.getUsername()+","+constructiveContributions+","+allContributions);			
		}	
		FileUtils.writeLines(new File(outFile), outputLines);
	}
}
