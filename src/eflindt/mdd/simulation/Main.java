package eflindt.mdd.simulation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import eflindt.mdd.simulation.Artifact.ArtifactVersion;

public class Main {
	
	static boolean debug = false;
	
	private static record Example(String description, Runnable runnable) {}
	
	private static final Map<Integer, Example> examples = new HashMap<>();
	
	static {
		examples.put(1, new Example("Ecosystem with manual co-evolution support where a meta model is changed", Main::example1));
		examples.put(2, new Example("Ecosystem with support for semi-automatic model and transformation co-evolution where a meta model is changed", Main::example2));
		examples.put(3, new Example("Ecosystem with support for semi-automatic model and transformation co-evolution where a platform is changed and migrated manually", Main::example3));
		examples.put(4, new Example("Ecosystem with transformation to same metamodel version, will create a loop", Main::example4));
	}
	
	static final void log(String message) {
		System.out.println(message);
	}
	
	private static final Repository repo = new RepositoryImpl();
	
	public static void main(String[] args) {
		if (args.length > 0) {
			if (args.length > 1 && "-d".equals(args[1])) {
				debug = true;
			}
			if ("-h".equals(args[0]) || "--help".equals(args[0])) {
				printHelp();
			} else {
				int index = Integer.parseInt(args[0]);
				if (examples.containsKey(index)) {
					Example example = examples.get(index);
					log(String.format("Executing example %s: %s", index, example.description()));
					example.runnable().run();
				} else {
					printHelp();
				}
			}
		} else {
			printHelp();
		}
	}
	
	private static void printHelp() {
		log("Provide an integer as the first argument to run the workflow for an example ecosystem");
		log("Use the -d flag as the second argument to get verbose output");
		examples.forEach((i, e) -> log(String.format("%s: %s", i, e.description())));
	}
	
	public static void onChange(Repository repo, ArtifactVersion changedArtifact) {
		Set<ArtifactVersion> metamodels = repo.getMetamodels(changedArtifact);
		// test if all consumers approve of this artifact
		boolean proceed = metamodels.isEmpty() || metamodels.stream()
			.map(repo::getConsumers).flatMap(Collection::stream)
			.map(repo::pull)
			.map(Artifact::asConsumer)
			.filter(Optional::isPresent).map(Optional::get)
			.allMatch(c -> c.test(changedArtifact));
		if (proceed) {
			for (ArtifactVersion metamodel : metamodels) {
				for (ArtifactVersion transformation : repo.getTransformations(metamodel)) {
					// execute each transformation with the changed artifact as input
					repo.pull(transformation).asTransformation().ifPresent(t -> t.accept(changedArtifact));
				}
			}
			for (ArtifactVersion inputMetamodel : repo.getInputs(changedArtifact)) {
				for (ArtifactVersion instance : repo.getInstances(inputMetamodel)) {
					// cast the changed artifact to a transformation and execute all instances with it
					repo.pull(changedArtifact).asTransformation().ifPresent(t -> t.accept(instance));
					// cast the changed artifact to a consumer and execute all instances with it
					repo.pull(changedArtifact).asConsumer().ifPresent(t -> t.test(instance));
				}
			}
		}
	}
	
	// basic setup
	private static final Artifact executable = ArtifactImpl.buildArtifact("executable").build();
	private static final Artifact deploymentPipeline = ConsumerImpl.buildConsumer("deploymentPipeline")
		.withInput(executable.version())
		.withConsumer(v -> {
			log("[DEPLOY] Integration testing and deploying " + v);
			return true;
		})
		.build();
	private static final Artifact sourceCode = ArtifactImpl.buildArtifact("sourceCode").build();
	private static final Artifact ecore = ArtifactImpl.buildArtifact("ecore").build();
	private static final Artifact trafoMM = ArtifactImpl.buildArtifact("trafoMM").build();
	
	// java stuff
	private static final Artifact java = ArtifactImpl.buildArtifact("java")
		.withMetamodel(sourceCode.version()).build();	
	private static final Artifact javaBuildPipeline = TransformationImpl.buildTransformation("javaBuildPipeline")
		.withInput(java.version())
		.withOutput(executable.version())
		.withTransformation(v -> {
			log("[BUILD] Unit testing and building " + v);
			Artifact m = repo.pull(v);
			repo.push(ArtifactImpl.buildArtifact(String.format("%sVer%s.jar", m.version().name(), m.version().version()))
				.withMetamodel(executable.version())
				.build());
		})
		.build();
	
	// platforms
	private static final Artifact springBootPlatform = ArtifactImpl.buildArtifact("springBoot").build();
	private static final Artifact dotNetPlatform = ArtifactImpl.buildArtifact("dotNet").build();
	private static final Artifact pythonPlatform = ArtifactImpl.buildArtifact("python").build();
	
	// microservice meta model and instances
	private static final Artifact microservice = ArtifactImpl.buildArtifact("microservice")
		.withMetamodel(ecore.version()).build();
	private static final Artifact customerMicroservice = ArtifactImpl.buildArtifact("customerMicroservice")
		.withMetamodel(microservice.version()).build();
	private static final Artifact shoppingCartMicroservice = ArtifactImpl.buildArtifact("shoppingCardMicroservice")
		.withMetamodel(microservice.version()).build();
	private static final Artifact orderMicroservice = ArtifactImpl.buildArtifact("orderMicroservice")
		.withMetamodel(microservice.version()).build();
	
	// microservice consumers
	private static final Artifact microserviceValidator = ConsumerImpl.buildConsumer("microserviceValidator")
		.withMetamodel(trafoMM.version())
		.withInput(microservice.version())
		.withConsumer(v -> {
			log("[CONSUME] Validating model of type microservice " + v);
			return true;
		})
		.build();
	private static final Artifact microserviceAnalyzer = ConsumerImpl.buildConsumer("microserviceAnalyzer")
		.withMetamodel(trafoMM.version())
		.withInput(microservice.version())
		.withConsumer(v -> {
			log("[CONSUME] Analyzing model of type microservice " + v);
			return true;
		})
		.build();
	private static final Artifact microserviceSimulator = ConsumerImpl.buildConsumer("microserviceSimulator")
		.withMetamodel(trafoMM.version())
		.withInput(microservice.version())
		.withConsumer(v -> {
			log("[CONSUME] Simulating model of type microservice " + v);
			return true;
		})
		.build();
	
	// microservice generators
	private static final Artifact microserviceToSpringBoot = TransformationImpl.buildTransformation("microserviceToSpringBoot")
		.withMetamodel(trafoMM.version())
		.withInput(microservice.version())
		.withOutput(springBootPlatform.version())
		.withTransformation(v -> {
			log("[M2T] Generating Spring Boot microservices for model " + v);
			repo.push(ArtifactImpl.buildArtifact(v.name() + "SpringBootGen")
				.withMetamodel(java.version()).build());
		})
		.build();
	private static final Artifact microserviceToDotNet = TransformationImpl.buildTransformation("microserviceToDotNet")
		.withMetamodel(trafoMM.version())
		.withInput(microservice.version())
		.withOutput(dotNetPlatform.version())
		.withTransformation(v -> {
			log("[M2T] Generating Dot Net microservices for model " + v);
			repo.push(ArtifactImpl.buildArtifact(v.name() + "DotNetGen")
				.withMetamodel(sourceCode.version()).build());
		})
		.build();
	private static final Artifact microserviceToPython = TransformationImpl.buildTransformation("microserviceToPython")
		.withMetamodel(trafoMM.version())
		.withInput(microservice.version())
		.withOutput(pythonPlatform.version())
		.withTransformation(v -> {
			log("[M2T] Generating Python microservices for model " + v);
			repo.push(ArtifactImpl.buildArtifact(v.name() + "PythonGen")
				.withMetamodel(sourceCode.version()).build());
		})
		.build();
	
	// generator validator
	private static final Artifact generatorValidator = ConsumerImpl.buildConsumer("generatorValidator")
		.withInput(trafoMM.version())
		.withConsumer(v -> {
			log("[CONSUME] Validating generator " + v);
			return true;
		})
		.build();
	
	// co-evolution support
	private static final Artifact coEvM = ArtifactImpl.buildArtifact("coEvM").build();
	private static final Artifact coEvModelGen = TransformationImpl.buildTransformation("coEvModelGen")
		.withInput(ecore.version())
		.withOutput(coEvM.version())
		.withTransformation(v -> {
			if (v.isInitialVersion()) {
				log("[CoEv] Don't create migration model for initial version of " + v);
			} else {
				log("[CoEv] Creating migration model for " + v);
				repo.push(CoEvolutionModelImpl.buildCoEvolutionModel(v.name() + "-coEvM")
					.withMetamodel(coEvM.version())
					.withChangedArtifact(v).build());
			}
		})
		.build();
	private static final Artifact modelCoEvGen = TransformationImpl.buildTransformation("modelCoEvGen")
		.withInput(coEvM.version())
		.withOutput(trafoMM.version())
		.withTransformation(v -> {
			Artifact m = repo.pull(v);
			if (m instanceof CoEvolutionModel coev) {
				ArtifactVersion changedArtifact = coev.getChangedArtifact();
				log("[CoEv] Creating model migration for " + changedArtifact);
				repo.push(TransformationImpl.buildTransformation(changedArtifact.name() + "-model-migration")
					// previous meta model version is the input
					.withInput(changedArtifact.decrement())
					// new meta model version is the output
					.withOutput(changedArtifact)
					.withTransformation(instanceVersion -> {
						// instances that are not conform to the previous version must not be migrated
						Artifact instance = repo.pull(instanceVersion);
						if (instance.getMetamodels().contains(changedArtifact.decrement())) {
							log(String.format("[M2M] Migrating model %s", instance.version()));
							// the migration must update the meta model to the changed model
							Artifact migratedInstance = Artifact.copyArtifact(instance)
								.updateMetamodel(changedArtifact).build();
							repo.push(migratedInstance);
						}						
					})
					.build());
			}
		}).build();
	private static final Artifact trafoCoEvGen = TransformationImpl.buildTransformation("trafoCoEvGen")
		.withInput(coEvM.version())
		.withOutput(trafoMM.version())
		.withTransformation(v -> {
			Artifact m = repo.pull(v);
			if (m instanceof CoEvolutionModel coev) {
				ArtifactVersion changedArtifact = coev.getChangedArtifact();
				log("[CoEv] Creating transformation migration for " + changedArtifact);
				repo.push(TransformationImpl.buildTransformation(changedArtifact.name() + "-transformation-migration")
					// signals that this transformation transforms other transformation
					// this is a higher order transformation
					.withInput(trafoMM.version())
					.withOutput(trafoMM.version())
					.withTransformation(tVersion -> {
						Artifact t = repo.pull(tVersion);
						// this condition is important to prevent a loop
						// only transformations that are dependent on the previous version must be migrated
						if (t.getInputs().contains(changedArtifact.decrement())
							|| t.getOutputs().contains(changedArtifact.decrement())) {
							log(String.format("[M2M] Migrating transformation %s", t.version()));
							// the migration must update the dependency to the changed model
							Artifact migratedTransformation = Artifact.copyArtifact(t)
								.updateDependency(changedArtifact).build();
							repo.push(migratedTransformation);
						}
					})
					.build());
			}
		}).build();
	
	public static void example1() {
		repo.push(executable, sourceCode, ecore, trafoMM, java, javaBuildPipeline, springBootPlatform, dotNetPlatform, pythonPlatform, microservice, microserviceToSpringBoot, microserviceToDotNet, customerMicroservice, shoppingCartMicroservice, orderMicroservice, microserviceToPython, generatorValidator, microserviceAnalyzer, microserviceSimulator, microserviceValidator, deploymentPipeline);
		log("### Changing microservice meta model and migrating Spring Boot generator manually:");
		repo.push(customerMicroservice);
		repo.push(microservice);
		repo.push(Artifact.copyArtifact(customerMicroservice)
			.updateMetamodel(microservice.version().increment()).build());
		repo.push(Artifact.copyArtifact(microserviceToSpringBoot)
			.updateDependency(microservice.version().increment()).build());
	}

	public static void example2() {
		repo.push(executable, sourceCode, ecore, trafoMM, java, javaBuildPipeline, springBootPlatform, dotNetPlatform, pythonPlatform, coEvModelGen, modelCoEvGen, trafoCoEvGen, microservice, microserviceToSpringBoot, microserviceToDotNet, customerMicroservice, shoppingCartMicroservice, orderMicroservice, microserviceToPython, generatorValidator, microserviceAnalyzer, microserviceSimulator, microserviceValidator, deploymentPipeline);
		// adding the meta model again will trigger the creation of a new version
		log("### Changing microservice meta model:");
		repo.push(microservice);
	}

	public static void example3() {
		repo.push(executable, sourceCode, ecore, trafoMM, java, javaBuildPipeline, springBootPlatform, dotNetPlatform, pythonPlatform, coEvModelGen, modelCoEvGen, trafoCoEvGen, microservice, microserviceToSpringBoot, microserviceToDotNet, customerMicroservice, shoppingCartMicroservice, orderMicroservice, microserviceToPython, generatorValidator, microserviceAnalyzer, microserviceSimulator, microserviceValidator, deploymentPipeline);
		log("### Changing Spring Boot platform:");
		// update the platform
		repo.push(springBootPlatform);
		// update the generator
		repo.push(Artifact.copyArtifact(microserviceToSpringBoot)
			.updateDependency(springBootPlatform.version().increment()).build());
	}

	public static void example4() {
		repo.push(microservice, microserviceToSpringBoot, customerMicroservice, shoppingCartMicroservice);
		log("### Creating transformation with the same meta model version as input and output:");
		repo.push(TransformationImpl.buildTransformation("loop").withInput(microservice.version()).withOutput(microservice.version()).withTransformation(v -> {
			log("Looping " + v);
			repo.push(repo.pull(v));
		}).build());
	}
	
}
