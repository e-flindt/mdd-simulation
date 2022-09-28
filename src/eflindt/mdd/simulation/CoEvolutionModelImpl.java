package eflindt.mdd.simulation;

import java.util.Set;

/**
 * An immutable base implementation of {@link CoEvolutionModel}.
 * 
 * @author Eric Flindt
 *
 */
public class CoEvolutionModelImpl extends ArtifactImpl implements CoEvolutionModel {

	private final ArtifactVersion changedArtifact;

	public CoEvolutionModelImpl(ArtifactVersion version, Set<ArtifactVersion> metamodels, Set<ArtifactVersion> inputs,
		Set<ArtifactVersion> outputs, ArtifactVersion changedArtifact) {
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
		if (Main.debug) {
			return String.format("%s; changedArtifact=%s", super.toString(), changedArtifact);
		}
		return super.toString();
	}

	public static CoEvolutionModelBuiler buildCoEvolutionModel(String name) {
		return new CoEvolutionModelBuiler(name);
	}

	public static CoEvolutionModelBuiler buildCoEvolutionModel(ArtifactVersion version) {
		return new CoEvolutionModelBuiler(version);
	}

	/**
	 * @author Eric Flindt
	 */
	public static class CoEvolutionModelBuiler
		extends AbstractArtifactBuilder<CoEvolutionModel, CoEvolutionModelBuiler> {

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

}