package eflindt.mdd.simulation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Main {
	
	private static boolean debug = false;
	
	private static record Example(String description, Runnable runnable) {}
	
	private static final Map<Integer, Example> examples = new HashMap<>();
	
	static {
		examples.put(1, new Example("Ecosystem with manual co-evolution support where a meta model is changed", Main::example1));
		examples.put(2, new Example("Ecosystem with support for semi-automatic model and transformation co-evolution where a meta model is changed", Main::example2));
		examples.put(3, new Example("Ecosystem with support for semi-automatic model and transformation co-evolution where a platform is changed", Main::example3));
		examples.put(4, new Example("Ecosystem with support for semi-automatic model and transformation co-evolution where the java version is changed", Main::example4));
		examples.put(5, new Example("Ecosystem with transformation to same metamodel version, will create a loop", Main::example5));
	}
	
	private static final void log(String message) {
		System.out.println(message);
	}
	
	private static final Repository repo = new RepositoryImpl();
	
	// basic setup
	private static final Artifact executable = buildArtifact("executable").build();
	private static final Artifact deploymentPipeline = buildConsumer("deploymentPipeline")
		.withInput(executable.version())
		.withConsumer(v -> {
			log("[DEPLOY] Integration testing and deploying " + v);
			return true;
		})
		.build();
	private static final Artifact sourceCode = buildArtifact("sourceCode").build();
	private static final Artifact ecore = buildArtifact("ecore").build();
	private static final Artifact trafoMM = buildArtifact("trafoMM").build();
	
	// java stuff
	private static final Artifact java = buildArtifact("java")
		.withMetamodel(sourceCode.version()).build();	
	private static final Artifact javaBuildPipeline = buildTransformation("javaBuildPipeline")
		.withInput(java.version())
		.withOutput(executable.version())
		.withTransformation(v -> {
			log("[BUILD] Unit testing and building " + v);
			Artifact m = repo.pull(v);
			repo.push(buildArtifact(String.format("%sVer%s.jar", m.version().name(), m.version().version()))
				.withMetamodel(executable.version())
				.build());
		})
		.build();
	
	// platforms
	private static final Artifact springBootPlatform = buildArtifact("springBoot").build();
	private static final Artifact dotNetPlatform = buildArtifact("dotNet").build();
	private static final Artifact pythonPlatform = buildArtifact("python").build();
	
	// microservice meta model and instances
	private static final Artifact microservice = buildArtifact("microservice")
		.withMetamodel(ecore.version()).build();
	private static final Artifact customerMicroservice = buildArtifact("customerMicroservice")
		.withMetamodel(microservice.version()).build();
	private static final Artifact shoppingCartMicroservice = buildArtifact("shoppingCardMicroservice")
		.withMetamodel(microservice.version()).build();
	private static final Artifact orderMicroservice = buildArtifact("orderMicroservice")
		.withMetamodel(microservice.version()).build();
	
	// microservice consumers
	private static final Artifact microserviceValidator = buildConsumer("microserviceValidator")
		.withInput(microservice.version())
		.withConsumer(v -> {
			log("[CONSUME] Validating model of type microservice " + v);
			return true;
		})
		.build();
	private static final Artifact microserviceAnalyzer = buildConsumer("microserviceAnalyzer")
		.withInput(microservice.version())
		.withConsumer(v -> {
			log("[CONSUME] Analyzing model of type microservice " + v);
			return true;
		})
		.build();
	private static final Artifact microserviceSimulator = buildConsumer("microserviceSimulator")
		.withInput(microservice.version())
		.withConsumer(v -> {
			log("[CONSUME] Simulating model of type microservice " + v);
			return true;
		})
		.build();
	
	// microservice generators
	private static final Artifact microserviceToSpringBoot = buildTransformation("microserviceToSpringBoot")
		.withMetamodel(trafoMM.version())
		.withInput(microservice.version())
		.withOutput(springBootPlatform.version())
		.withTransformation(v -> {
			log("[M2T] Generating Spring Boot microservices for model " + v);
			repo.push(buildArtifact(v.name() + "SpringBootGen")
				.withMetamodel(java.version()).build());
		})
		.build();
	private static final Artifact microserviceToDotNet = buildTransformation("microserviceToDotNet")
		.withMetamodel(trafoMM.version())
		.withInput(microservice.version())
		.withOutput(dotNetPlatform.version())
		.withTransformation(v -> {
			log("[M2T] Generating Dot Net microservices for model " + v);
			repo.push(buildArtifact(v.name() + "DotNetGen")
				.withMetamodel(sourceCode.version()).build());
		})
		.build();
	private static final Artifact microserviceToPython = buildTransformation("microserviceToPython")
		.withMetamodel(trafoMM.version())
		.withInput(microservice.version())
		.withOutput(pythonPlatform.version())
		.withTransformation(v -> {
			log("[M2T] Generating Python microservices for model " + v);
			repo.push(buildArtifact(v.name() + "PythonGen")
				.withMetamodel(sourceCode.version()).build());
		})
		.build();
	
	// generator validator
	private static final Artifact generatorValidator = buildConsumer("generatorValidator")
		.withInput(trafoMM.version())
		.withConsumer(v -> {
			log("[CONSUME] Validating generator " + v);
			return true;
		})
		.build();
	
	// co-evolution support
	private static final Artifact coEvM = buildArtifact("coEvM").build();
	private static final Artifact coEvModelGen = buildTransformation("coEvModelGen")
		.withInput(ecore.version())
		.withOutput(coEvM.version())
		.withTransformation(v -> {
			if (v.isInitialVersion()) {
				log("[CoEv] Don't create migration model for initial version of " + v);
			} else {
				log("[CoEv] Creating migration model for " + v);
				repo.push(buildCoEvolutionModel(v.name() + "-coEvM")
					.withMetamodel(coEvM.version())
					.withChangedArtifact(v).build());
			}
		})
		.build();
	private static final Artifact modelCoEvGen = buildTransformation("modelCoEvGen")
		.withInput(coEvM.version())
		.withOutput(trafoMM.version())
		.withTransformation(v -> {
			Artifact m = repo.pull(v);
			if (m instanceof CoEvolutionModel coev) {
				ArtifactVersion changedArtifact = coev.getChangedArtifact();
				log("[CoEv] Creating model migration for " + changedArtifact);
				repo.push(buildTransformation(changedArtifact.name() + "-model-migration")
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
							Artifact migratedInstance = copyArtifact(instance)
								.updateMetamodel(changedArtifact).build();
							repo.push(migratedInstance);
						}						
					})
					.build());
			}
		}).build();
	private static final Artifact trafoCoEvGen = buildTransformation("trafoCoEvGen")
		.withInput(coEvM.version())
		.withOutput(trafoMM.version())
		.withTransformation(v -> {
			Artifact m = repo.pull(v);
			if (m instanceof CoEvolutionModel coev) {
				ArtifactVersion changedArtifact = coev.getChangedArtifact();
				log("[CoEv] Creating transformation migration for " + changedArtifact);
				repo.push(buildTransformation(changedArtifact.name() + "-transformation-migration")
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
							Artifact migratedTransformation = copyArtifact(t)
								.updateDependency(changedArtifact).build();
							repo.push(migratedTransformation);
						}
					})
					.build());
			}
		}).build();
	
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
	
	public static void example1() {
		repo.push(executable, sourceCode, ecore, trafoMM, java, javaBuildPipeline, springBootPlatform, dotNetPlatform, pythonPlatform, microservice, microserviceToSpringBoot, microserviceToDotNet, customerMicroservice, shoppingCartMicroservice, orderMicroservice, microserviceToPython, generatorValidator, microserviceAnalyzer, microserviceSimulator, microserviceValidator, deploymentPipeline);
		log("### Changing microservice meta model and migrating Spring Boot generator manually:");
		repo.push(customerMicroservice);
		repo.push(microservice);
		repo.push(copyArtifact(customerMicroservice)
			.updateMetamodel(microservice.version().increment()).build());
		repo.push(copyArtifact(microserviceToSpringBoot)
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
		repo.push(copyArtifact(microserviceToSpringBoot)
			.updateDependency(springBootPlatform.version().increment()).build());
	}

	public static void example4() {
		repo.push(executable, sourceCode, ecore, trafoMM, java, javaBuildPipeline, springBootPlatform, dotNetPlatform, pythonPlatform, coEvModelGen, modelCoEvGen, trafoCoEvGen, microservice, microserviceToSpringBoot, microserviceToDotNet, customerMicroservice, shoppingCartMicroservice, orderMicroservice, microserviceToPython, generatorValidator, microserviceAnalyzer, microserviceSimulator, microserviceValidator, deploymentPipeline);
		log("### Changing Java version and migrating Spring Boot platform, Java build pipeline and Spring Boot Generator manually:");
		// update java
		repo.push(java);
		// manual co-evolution
		repo.push(copyArtifact(springBootPlatform)
			.updateMetamodel(java.version().increment()).build());
		repo.push(copyArtifact(javaBuildPipeline)
			.updateDependency(java.version().increment()).build());
		repo.push(copyArtifact(microserviceToSpringBoot)
			.updateDependency(springBootPlatform.version().increment()).build());
	}

	public static void example5() {
		repo.push(microservice, microserviceToSpringBoot, customerMicroservice, shoppingCartMicroservice);
		log("### Creating transformation with the same meta model version as input and output:");
		repo.push(buildTransformation("loop").withInput(microservice.version()).withOutput(microservice.version()).withTransformation(v -> {
			log("Looping " + v);
			repo.push(repo.pull(v));
		}).build());
	}
	
	public static void onChange(Repository repo, ArtifactVersion version) {
		Set<ArtifactVersion> metamodels = repo.getMetamodels(version);
		boolean proceed = metamodels.isEmpty() || metamodels.stream()
			.map(repo::getConsumers).flatMap(Collection::stream)
			.map(repo::pull)
			.map(Artifact::asConsumer)
			.filter(Optional::isPresent).map(Optional::get)
			.allMatch(c -> c.getConsumer().test(version));
		if (proceed) {
			for (ArtifactVersion metamodel : metamodels) {
				for (ArtifactVersion transformation : repo.getTransformations(metamodel)) {
					repo.pull(transformation).asTransformation().ifPresent(t -> t.accept(version));
				}
			}
			for (ArtifactVersion inputMetamodel : repo.getInputs(version)) {
				for (ArtifactVersion instance : repo.getInstances(inputMetamodel)) {
					repo.pull(version).asTransformation().ifPresent(t -> t.accept(instance));
				}
			}
		}
	}
	
	public static interface Artifact {
		
		ArtifactVersion version();
		
		Set<ArtifactVersion> getMetamodels();
		
		Set<ArtifactVersion> getInputs();
		
		Set<ArtifactVersion> getOutputs();
		
		Optional<ModelTransformation> asTransformation();
		
		Optional<ModelConsumer> asConsumer();
		
	}
	
	public record ArtifactVersion(String name, int version) {
		
		public ArtifactVersion increment() {
			return new ArtifactVersion(name, version + 1);
		}
		
		public ArtifactVersion decrement() {
			if (isInitialVersion()) {
				throw new IllegalStateException("Can't create previous version for initial version");
			}
			return new ArtifactVersion(name, version - 1);
		}
		
		public boolean isInitialVersion() {
			return version == 0;
		}
		
	}
	
	public static interface ModelTransformation extends Artifact, Consumer<ArtifactVersion> {
		
		Consumer<ArtifactVersion> getTransformation();
		
	}
	
	public static interface ModelConsumer extends Artifact {
		
		Predicate<ArtifactVersion> getConsumer();
		
	}
	
	public static interface CoEvolutionModel extends Artifact {
		
		ArtifactVersion getChangedArtifact();
		
	}
	
	public static class ArtifactImpl implements Artifact {

		private final ArtifactVersion version;
		private final Set<ArtifactVersion> metamodels;
		private final Set<ArtifactVersion> inputs;
		private final Set<ArtifactVersion> outputs;
		
		public ArtifactImpl(ArtifactVersion version, Set<ArtifactVersion> metamodels, Set<ArtifactVersion> inputs, Set<ArtifactVersion> outputs) {
			this.version = version;
			this.metamodels = Collections.unmodifiableSet(metamodels);
			this.inputs = Collections.unmodifiableSet(inputs);
			this.outputs = Collections.unmodifiableSet(outputs);
		}
		
		@Override
		public ArtifactVersion version() {
			return version;
		}
		
		@Override
		public Set<ArtifactVersion> getMetamodels() {
			return metamodels;
		}

		@Override
		public Set<ArtifactVersion> getInputs() {
			return inputs;
		}
		
		@Override
		public Set<ArtifactVersion> getOutputs() {
			return outputs;
		}
		
		@Override
		public Optional<ModelTransformation> asTransformation() {
			return Optional.ofNullable(this)
				.filter(ModelTransformation.class::isInstance)
				.map(ModelTransformation.class::cast);
		}
		
		@Override
		public Optional<ModelConsumer> asConsumer() {
			return Optional.ofNullable(this)
				.filter(ModelConsumer.class::isInstance)
				.map(ModelConsumer.class::cast);
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof Artifact)) {
				return false;
			}
			Artifact other = (Artifact) o;
			return Objects.equals(version, other.version());
		}
		
		@Override
		public String toString() {
			if (debug) {
				return String.format("%s; metamodels=%s; inputs=%s; outputs=%s", version, metamodels, inputs, outputs);
			}
			return version.toString();
		}
		
	}
	
	public static class TransformationImpl extends ArtifactImpl implements ModelTransformation {
		
		private final Consumer<ArtifactVersion> transformation;
		
		public TransformationImpl(ArtifactVersion version, Set<ArtifactVersion> metamodels, Set<ArtifactVersion> inputs, Set<ArtifactVersion> outputs, Consumer<ArtifactVersion> transformation) {
			super(version, metamodels, inputs, outputs);
			this.transformation = transformation;
		}
		
		@Override
		public Consumer<ArtifactVersion> getTransformation() {
			return transformation;
		}
		
		@Override
		public void accept(ArtifactVersion t) {
			transformation.accept(t);
		}
		
		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}
		
	}
	
	public static class ConsumerImpl extends ArtifactImpl implements ModelConsumer {
		
		private final Predicate<ArtifactVersion> consumer;
		
		public ConsumerImpl(ArtifactVersion version, Set<ArtifactVersion> metamodels, Set<ArtifactVersion> inputs, Set<ArtifactVersion> outputs, Predicate<ArtifactVersion> consumer) {
			super(version, metamodels, inputs, outputs);
			this.consumer = consumer;
		}

		@Override
		public Predicate<ArtifactVersion> getConsumer() {
			return consumer;
		}
		
		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}
		
	}
	
	public static class CoEvolutionModelImpl extends ArtifactImpl implements CoEvolutionModel {
		
		private final ArtifactVersion changedArtifact;
		
		public CoEvolutionModelImpl(ArtifactVersion version, Set<ArtifactVersion> metamodels, Set<ArtifactVersion> inputs, Set<ArtifactVersion> outputs, ArtifactVersion changedArtifact) {
			super(version, metamodels, inputs, outputs);
			this.changedArtifact = changedArtifact;
		}
		
		@Override
		public ArtifactVersion getChangedArtifact() {
			return changedArtifact;
		}
		
		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}
		
		@Override
		public String toString() {
			if (debug) {
				return String.format("%s; changedArtifact=%s", super.toString(), changedArtifact);
			}
			return super.toString();
		}
		
	}
	
	public interface Repository {

		Artifact pull(ArtifactVersion version);

		Set<ArtifactVersion> getInstances(ArtifactVersion version);

		Set<ArtifactVersion> getMetamodels(ArtifactVersion version);

		Set<ArtifactVersion> getInputs(ArtifactVersion version);

		Set<ArtifactVersion> getTransformations(ArtifactVersion version);
		
		Set<ArtifactVersion> getConsumers(ArtifactVersion version);

		void push(Artifact a);

		void push(Artifact... a);

	}
	
	public static class RepositoryImpl implements Repository {
		
		private Map<ArtifactVersion, Artifact> artifactsByVersion = new HashMap<>();
		
		@Override
		public Artifact pull(ArtifactVersion version) {
			return artifactsByVersion.get(version);
		}
		
		@Override
		public Set<ArtifactVersion> getInstances(ArtifactVersion version) {
			return artifactsByVersion.values().stream()
				// find any model that has declared the argument as meta model
				.filter(m1 -> m1.getMetamodels().contains(version))
				.map(Artifact::version)
				.collect(Collectors.toSet());
		}
		
		@Override
		public Set<ArtifactVersion> getMetamodels(ArtifactVersion version) {
			return Optional.ofNullable(version)
				.map(artifactsByVersion::get)
				.map(Artifact::getMetamodels)
				.orElse(Collections.emptySet());
		}
		
		@Override
		public Set<ArtifactVersion> getInputs(ArtifactVersion version) {
			return Optional.ofNullable(version)
				.map(artifactsByVersion::get)
				.map(Artifact::getInputs)
				.orElse(Collections.emptySet());
		}
		
		@Override
		public Set<ArtifactVersion> getTransformations(ArtifactVersion version) {
			return artifactsByVersion.values().stream()
				// find any transformation that has declared the argument as an input and something as its output
				.filter(t -> t.getInputs().contains(version) && !t.getOutputs().isEmpty())
				.map(Artifact::version)
				.collect(Collectors.toSet());
		}
		
		@Override
		public Set<ArtifactVersion> getConsumers(ArtifactVersion version) {
			return artifactsByVersion.values().stream()
				// find any consumer that has declared the argument as an input and nothing as its output
				.filter(t -> t.getInputs().contains(version) && t.getOutputs().isEmpty())
				.map(Artifact::version)
				.collect(Collectors.toSet());
		}
		
		@Override
		public void push(Artifact a) {
			ArtifactVersion version = a.version();
			while (artifactsByVersion.containsKey(version)) {
				version = version.increment();
			}			
			Artifact newVersion = copyArtifact(a).withVersion(version).build();
			artifactsByVersion.put(newVersion.version(), newVersion);
			log("[PUSH] " + newVersion);
			onChange(this, newVersion.version());
		}
		
		@Override
		public void push(Artifact... a) {
			Arrays.asList(a).forEach(this::push);
		}
		
	}
	
	public abstract static class AbstractArtifactBuilder<T extends Artifact, U extends AbstractArtifactBuilder<T, U>> {
		
		protected ArtifactVersion version;
		
		protected final Set<ArtifactVersion> metamodels = new HashSet<>();
		
		protected final Set<ArtifactVersion> inputs = new HashSet<>();
		
		protected final Set<ArtifactVersion> outputs = new HashSet<>();
		
		protected abstract U getThis();
		
		public U withVersion(ArtifactVersion version) {
			this.version = version;
			return getThis();
		}
		
		public U withMetamodel(ArtifactVersion metamodel) {
			this.metamodels.add(metamodel);
			return getThis();
		}
		
		public U updateMetamodel(ArtifactVersion version) {
			if (this.metamodels.remove(version.decrement())) {
				this.metamodels.add(version);
			}
			return getThis();
		}
		
		public U withInput(ArtifactVersion input) {
			this.inputs.add(input);
			return getThis();
		}
		
		public U withOutput(ArtifactVersion output) {
			this.outputs.add(output);
			return getThis();
		}
		
		public U updateDependency(ArtifactVersion version) {
			if (this.inputs.remove(version.decrement())) {
				this.inputs.add(version);
			}
			if (this.outputs.remove(version.decrement())) {
				this.outputs.add(version);
			}
			return getThis();
		}
		
		public abstract T build();
		
	}
	
	public static ArtifactBuilder buildArtifact(String name) {
		return new ArtifactBuilder(name);
	}
	
	public static ArtifactBuilder buildArtifact(ArtifactVersion version) {
		return new ArtifactBuilder(version);
	}
	
	public static class ArtifactBuilder extends AbstractArtifactBuilder<Artifact, ArtifactBuilder> {
		
		public ArtifactBuilder(String name) {
			this.version = new ArtifactVersion(name, 0);
		}
		
		public ArtifactBuilder(ArtifactVersion version) {
			this.version = version;
		}
		
		@Override
		protected ArtifactBuilder getThis() {
			return this;
		}

		@Override
		public Artifact build() {
			return new ArtifactImpl(version, metamodels, inputs, outputs);
		}
		
	}
	
	public static TransformationBuilder buildTransformation(String name) {
		return new TransformationBuilder(name);
	}
	
	public static TransformationBuilder buildTransformation(ArtifactVersion version) {
		return new TransformationBuilder(version);
	}
	
	public static class TransformationBuilder extends AbstractArtifactBuilder<ModelTransformation, TransformationBuilder> {
		
		private Consumer<ArtifactVersion> transformation;
		
		public TransformationBuilder(String name) {
			this.version = new ArtifactVersion(name, 0);
		}
		
		public TransformationBuilder(ArtifactVersion version) {
			this.version = version;
		}
		
		public TransformationBuilder withTransformation(Consumer<ArtifactVersion> transformation) {
			this.transformation = transformation;
			return this;
		}
		
		@Override
		protected TransformationBuilder getThis() {
			return this;
		}

		@Override
		public ModelTransformation build() {
			return new TransformationImpl(version, metamodels, inputs, outputs, transformation);
		}
		
	}
	
	public static ConsumerBuilder buildConsumer(String name) {
		return new ConsumerBuilder(name);
	}
	
	public static ConsumerBuilder buildConsumer(ArtifactVersion version) {
		return new ConsumerBuilder(version);
	}
	
	public static class ConsumerBuilder extends AbstractArtifactBuilder<ModelConsumer, ConsumerBuilder> {
		
		private Predicate<ArtifactVersion> consumer;
		
		public ConsumerBuilder(String name) {
			this.version = new ArtifactVersion(name, 0);
		}
		
		public ConsumerBuilder(ArtifactVersion version) {
			this.version = version;
		}
		
		public ConsumerBuilder withConsumer(Predicate<ArtifactVersion> consumer) {
			this.consumer = consumer;
			return this;
		}
		
		@Override
		protected ConsumerBuilder getThis() {
			return this;
		}

		@Override
		public ModelConsumer build() {
			return new ConsumerImpl(version, metamodels, inputs, outputs, consumer);
		}
		
	}
	
	public static CoEvolutionModelBuiler buildCoEvolutionModel(String name) {
		return new CoEvolutionModelBuiler(name);
	}
	
	public static CoEvolutionModelBuiler buildCoEvolutionModel(ArtifactVersion version) {
		return new CoEvolutionModelBuiler(version);
	}
	
	public static class CoEvolutionModelBuiler extends AbstractArtifactBuilder<CoEvolutionModel, CoEvolutionModelBuiler> {
		
		private ArtifactVersion changedArtifact;
		
		public CoEvolutionModelBuiler(String name) {
			this.version = new ArtifactVersion(name, 0);
		}
		
		public CoEvolutionModelBuiler(ArtifactVersion version) {
			this.version = version;
		}
		
		public CoEvolutionModelBuiler withChangedArtifact(ArtifactVersion changedArtifact) {
			this.changedArtifact = changedArtifact;
			return this;
		}
		
		@Override
		protected CoEvolutionModelBuiler getThis() {
			return this;
		}

		@Override
		public CoEvolutionModel build() {
			return new CoEvolutionModelImpl(version, metamodels, inputs, outputs, changedArtifact);
		}
		
	}
	
	public static AbstractArtifactBuilder<?, ?> copyArtifact(Artifact artifact) {
		AbstractArtifactBuilder<?, ?> builder;
		if (artifact instanceof CoEvolutionModel coevm) {
			builder = buildCoEvolutionModel(coevm.version()).withChangedArtifact(coevm.getChangedArtifact());
		} else if (artifact instanceof ModelTransformation t) {
			builder = buildTransformation(t.version()).withTransformation(t.getTransformation());
		} else if (artifact instanceof ModelConsumer c) {
			builder = buildConsumer(c.version()).withConsumer(c.getConsumer());
		} else {
			builder = buildArtifact(artifact.version());
		}
		artifact.getMetamodels().forEach(builder::withMetamodel);
		artifact.getInputs().forEach(builder::withInput);
		artifact.getOutputs().forEach(builder::withOutput);
		return builder;
	}
	
}
