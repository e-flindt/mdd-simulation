package eflindt.mdd.simulation;

import java.util.Set;
import java.util.function.Consumer;
/**
 * An immutable base implementation of {@link ModelTransformation}.
 * 
 * @author Eric Flindt
 *
 */
public class TransformationImpl extends ArtifactImpl implements ModelTransformation {
	
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
	
	public static TransformationBuilder buildTransformation(String name) {
		return new TransformationBuilder(name);
	}
	
	public static TransformationBuilder buildTransformation(ArtifactVersion version) {
		return new TransformationBuilder(version);
	}
	
	/**
	 * @author Eric Flindt
	 */
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
	
}