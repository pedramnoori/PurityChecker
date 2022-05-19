package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

public class UMLImportListDiff {
	private Set<String> removedImports;
	private Set<String> addedImports;
	private Set<String> commonImports;
	private Set<Pair<String, String>> changedImports;
	
	public UMLImportListDiff(List<String> oldImports, List<String> newImports) {
		this.changedImports = new LinkedHashSet<>();
		Set<String> oldImportSet = new LinkedHashSet<>(oldImports);
		Set<String> newImportSet = new LinkedHashSet<>(newImports);
		Set<String> intersection = new LinkedHashSet<>();
		intersection.addAll(oldImportSet);
		intersection.retainAll(newImportSet);
		this.commonImports = intersection;
		oldImportSet.removeAll(intersection);
		this.removedImports = oldImportSet;
		newImportSet.removeAll(intersection);
		this.addedImports = newImportSet;
	}

	public void findImportChanges(String nameBefore, String nameAfter) {
		if(removedImports.contains(nameBefore) && addedImports.contains(nameAfter)) {
			Pair<String, String> pair = Pair.of(nameBefore, nameAfter);
			changedImports.add(pair);
			removedImports.remove(nameBefore);
			addedImports.remove(nameAfter);
		}
		Set<String> matchedRemovedStaticImports = new LinkedHashSet<>();
		for(String removedImport : removedImports) {
			if(removedImport.startsWith(nameBefore + ".")) {
				matchedRemovedStaticImports.add(removedImport);
			}
		}
		Set<String> matchedAddedStaticImports = new LinkedHashSet<>();
		for(String addedImport : addedImports) {
			if(addedImport.startsWith(nameAfter + ".")) {
				matchedAddedStaticImports.add(addedImport);
			}
		}
		for(String removedImport : matchedRemovedStaticImports) {
			for(String addedImport : matchedAddedStaticImports) {
				String suffix1 = removedImport.substring(nameBefore.length());
				String suffix2 = addedImport.substring(nameAfter.length());
				if(suffix1.equals(suffix2)) {
					Pair<String, String> pair = Pair.of(removedImport, addedImport);
					changedImports.add(pair);
					removedImports.remove(removedImport);
					addedImports.remove(addedImport);
					break;
				}
			}
		}
	}

	public Set<String> getRemovedImports() {
		return removedImports;
	}

	public Set<String> getAddedImports() {
		return addedImports;
	}

	public Set<String> getCommonImports() {
		return commonImports;
	}

	public Set<Pair<String, String>> getChangedImports() {
		return changedImports;
	}
}
