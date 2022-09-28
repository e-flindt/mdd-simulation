package eflindt.mdd.simulation;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * A class describing an arbitrary artifact inside a version control system.
 * 
 * Artifacts are meant to be immutable, therefore new version are created by
 * copying the old one into a builder, which then can be modified.
 * Implementations must provide a copy mechanism by modifying
 * {@link Artifact#copyArtifact(Artifact)} for their sub class.
 * 
 * @author Eric Flindt
 *
 */
public interface Artifact {

	/**
	 * @return The {@link ArtifactVersion}
	 */
	ArtifactVersion version();

	/**
	 * @return The meta models this artifact claims to be conform to
	 */
	Set<ArtifactVersion> getMetamodels();

	/**
	 * @return The input meta model of which instances instances can be accepted
	 */
	Set<ArtifactVersion> getInputs();

	/**
	 * @return The output meta model of which instances will be produced
	 */
	Set<ArtifactVersion> getOutputs();

	/**
	 * @return Down cast this as a transformation
	 */
	Optional<ModelTransformation> asTransformation();

	/**
	 * @return Down cast this as a consumer
	 */
	Optional<ModelConsumer> asConsumer();
	
	/**
	 * This record represents version information for an artifact.
	 * 
	 * @author Eric Flindt
	 *
	 */
	public static record ArtifactVersion(String name, int version) {

		/**
		 * @return An {@link ArtifactVersion} with an incremented version number.
		 */
		public ArtifactVersion increment() {
			return new ArtifactVersion(name, version + 1);
		}

		/**
		 * @return An {@link ArtifactVersion} with a decremented version number.
		 */
		public ArtifactVersion decrement() {
			if (isInitialVersion()) {
				throw new IllegalStateException("Can't create previous version for initial version");
			}
			return new ArtifactVersion(name, version - 1);
		}

		/**
		 * @return <code>true</code> if this version number is zero.
		 */
		public boolean isInitialVersion() {
			return version == 0;
		}

	}

	/**
	 * All sub classes of {@link Artifact} must extend this method to include a
	 * specific builder for the sub class.
	 * 
	 * @param artifact The {@link Artifact} to be copied.
	 * @return An {@link AbstractArtifactBuilder} with properties of the provided
	 *         artifact.
	 */
	public static AbstractArtifactBuilder<?, ?> copyArtifact(Artifact artifact) {
		AbstractArtifactBuilder<?, ?> builder;
		if (artifact instanceof CoEvolutionModel coevm) {
			builder = CoEvolutionModelImpl.buildCoEvolutionModel(coevm.version())
				.withChangedArtifact(coevm.getChangedArtifact());
		} else if (artifact instanceof ModelTransformation t) {
			builder = TransformationImpl.buildTransformation(t.version()).withTransformation(t.getTransformation());
		} else if (artifact instanceof ModelConsumer c) {
			builder = ConsumerImpl.buildConsumer(c.version()).withConsumer(c.getConsumer());
		} else {
			builder = ArtifactImpl.buildArtifact(artifact.version());
		}
		artifact.getMetamodels().forEach(builder::withMetamodel);
		artifact.getInputs().forEach(builder::withInput);
		artifact.getOutputs().forEach(builder::withOutput);
		return builder;
	}
	
	/**
	 * @author Eric Flindt
	 */
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

}