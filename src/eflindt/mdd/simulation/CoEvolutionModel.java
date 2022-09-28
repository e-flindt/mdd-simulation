package eflindt.mdd.simulation;

/**
 * A specific {@link Artifact} representing a co-evolution model containing
 * information about the artifact that triggered the co-evolution.
 * 
 * @author Eric Flindt
 *
 */
public interface CoEvolutionModel extends Artifact {

	/**
	 * @return The {@link Artifact} that triggered the co-evolution.
	 */
	ArtifactVersion getChangedArtifact();

}