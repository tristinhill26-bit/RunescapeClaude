package rs117.hd.interaction;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum HintArrowMode
{
	OFF("Off"),
	BEAM("Beam"),
	BEAM_AND_LIGHT("Beam & light");

	private final String name;

	@Override
	public String toString() {
		return name;
	}
}
