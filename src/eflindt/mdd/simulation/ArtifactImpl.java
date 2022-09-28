package eflindt.mdd.simulation;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * An immutable base implementation of {@link Artifact}.
 * 
 * @author Eric Flindt
 *
 */
public class ArtifactImpl implements Artifact {

	private final ArtifactVersion version;
	private final Set<ArtifactVersion> metamodels;
	private final Set<ArtifactVersion> inputs;
	private final Set<ArtifactVersion> outputs;

	public ArtifactImpl(ArtifactVersion version, Set<ArtifactVersion> metamodels, Set<ArtifactVersion> inputs,
		Set<ArtifactVersion> outputs) {
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
		return Optional.ofNullable(this).filter(ModelTransformation.class::isInstance)
			.map(ModelTransformation.class::cast);
	}

	@Override
	public Optional<ModelConsumer> asConsumer() {
		return Optional.ofNullable(this).filter(ModelConsumer.class::isInstance).map(ModelConsumer.class::cast);
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
		if (Main.debug) {
			return String.format("%s; metamodels=%s; inputs=%s; outputs=%s", version, metamodels, inputs, outputs);
		}
		return version.toString();
	}

	public static ArtifactBuilder buildArtifact(String name) {
		return new ArtifactBuilder(name);
	}

	public static ArtifactBuilder buildArtifact(ArtifactVersion version) {
		return new ArtifactBuilder(version);
	}

	/**
	 * @author Eric Flindt
	 */
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

}