/**
 * 
 */
package de.bodden.tamiflex.reflectionview.views;

import static de.bodden.tamiflex.reflectionview.views.TreeObject.INVISIBLE_ROOT_NODE;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IViewSite;


public class ReflectionViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {

	private final IViewSite viewSite;
	private final ReflectionView reflectionView;

	public ReflectionViewContentProvider(IViewSite viewSite, ReflectionView reflectionView) {
		this.viewSite = viewSite;
		this.reflectionView = reflectionView;
	}

	public void dispose() {
	}
	public Object[] getElements(Object parent) {
		if (parent.equals(viewSite)) {
			if (INVISIBLE_ROOT_NODE==null) initialize();
			return getChildren(INVISIBLE_ROOT_NODE);
		}
		return getChildren(parent);
	}
	public Object getParent(Object child) {
		return ((TreeObject)child).getParent();
	}
	public Object [] getChildren(Object parent) {
		if(parent==null) return new Object[0];
		return ((TreeObject)parent).getChildren();
	}
	public boolean hasChildren(Object parent) {
		if(parent==null) return false;
		return ((TreeObject)parent).hasChildren();
	}

	public void initialize() {
		Set<IPath> traceFilePaths = reflectionView.getCurrentTraceFiles();
		for (IPath traceFilePath : traceFilePaths) {
			if(traceFilePath==null) continue;
			IPath relativePath = traceFilePath.makeRelativeTo(ResourcesPlugin.getWorkspace().getRoot().getLocation());
			
			if (INVISIBLE_ROOT_NODE.hasChildren()) {
				for(TreeObject o: INVISIBLE_ROOT_NODE.getChildren()) {
					TraceFileNode n = (TraceFileNode)o;
					if(n.getName().equals(relativePath.toString())) {
						INVISIBLE_ROOT_NODE.removeChild(n);
						break;
					}					
				}
			}
			TraceFileNode fileNode = new TraceFileNode(relativePath);
			INVISIBLE_ROOT_NODE.addChild(fileNode);
			ReflectionViewContentInserter contentInserter = new ReflectionViewContentInserter(fileNode, reflectionView);
			try {
				InputStream inputStream = traceFilePath.toFile().toURI().toURL().openStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
				String line;
				int lines = 0;
				
				while((line=reader.readLine())!=null) {
					contentInserter.insertFromTraceFileLine(line);
					lines++;
				}
			} catch (FileNotFoundException e) {
				throw new RuntimeException("Trace file not found.",e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			for(TreeObject node: fileNode.getChildren()) {
				if(!((TreeParent)node).hasChildren()) {
					fileNode.removeChild(node);
				} 
			}
		}
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
	
	public TreeParent getRoot() {
		return INVISIBLE_ROOT_NODE;
	}
}