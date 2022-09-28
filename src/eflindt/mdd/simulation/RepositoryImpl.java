package eflindt.mdd.simulation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import eflindt.mdd.simulation.Artifact.ArtifactVersion;

/**
 * An implementation of {@link Repository} that copies and increments the
 * version each time an artifact is pushed.
 * 
 * @author Eric Flindt
 *
 */
public class RepositoryImpl implements Repository {

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
			// find any transformation that has declared the argument as an input and
			// something as its output
			.filter(t -> t.getInputs().contains(version) && !t.getOutputs().isEmpty())
			.map(Artifact::version)
			.collect(Collectors.toSet());
	}

	@Override
	public Set<ArtifactVersion> getConsumers(ArtifactVersion version) {
		return artifactsByVersion.values().stream()
			// find any consumer that has declared the argument as an input and nothing as
			// its output
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
		Artifact newVersion = Artifact.copyArtifact(a).withVersion(version).build();
		artifactsByVersion.put(newVersion.version(), newVersion);
		Main.log("[PUSH] " + newVersion);
		Main.onChange(this, newVersion.version());
	}

	@Override
	public void push(Artifact... a) {
		Arrays.asList(a).forEach(this::push);
	}

}