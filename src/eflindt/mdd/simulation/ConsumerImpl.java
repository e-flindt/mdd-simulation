package eflindt.mdd.simulation;

import java.util.Set;
import java.util.function.Predicate;

/**
 * An immutable base implementation of {@link ModelConsumer}.
 * 
 * @author Eric Flindt
 *
 */
public class ConsumerImpl extends ArtifactImpl implements ModelConsumer {

	private final Predicate<ArtifactVersion> consumer;

	public ConsumerImpl(ArtifactVersion version, Set<ArtifactVersion> metamodels, Set<ArtifactVersion> inputs,
		Set<ArtifactVersion> outputs, Predicate<ArtifactVersion> consumer) {
		super(version, metamodels, inputs, outputs);
		this.consumer = consumer;
	}

	@Override
	public Predicate<ArtifactVersion> getConsumer() {
		return consumer;
	}
	
	@Override
	public boolean test(ArtifactVersion t) {
		return consumer.test(t);
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	public static ConsumerBuilder buildConsumer(String name) {
		return new ConsumerBuilder(name);
	}

	public static ConsumerBuilder buildConsumer(ArtifactVersion version) {
		return new ConsumerBuilder(version);
	}

	/**
	 * @author Eric Flindt
	 */
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

}