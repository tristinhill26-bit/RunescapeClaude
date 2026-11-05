package rs117.hd.interaction;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ScanLightMode
{
	EVERYTHING_EXAMINABLE("Everything examinable"),
	CATEGORIES_ONLY("Only selected categories");

	private final String name;

	@Override
	public String toString() {
		return name;
	}
}
