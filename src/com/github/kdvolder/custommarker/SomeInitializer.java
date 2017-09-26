/*******************************************************************************
 * Copyright (c) 2017 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package com.github.kdvolder.custommarker;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.springsource.ide.eclipse.commons.livexp.core.DelegatingLiveExp;
import org.springsource.ide.eclipse.commons.livexp.core.LiveExpression;
import org.springsource.ide.eclipse.commons.livexp.core.LiveVariable;
import org.springsource.ide.eclipse.commons.livexp.ui.Disposable;

@SuppressWarnings("restriction")
public class SomeInitializer implements IStartup {

	private static final String MARK_WORD = "boot";

	private static final String ANNOTION_TYPE = "org.springframework.tooling.bootinfo";

	public static LiveVariable<IWorkbenchPage> activePage = new LiveVariable<>();
	
	private List<Disposable> disposables = new ArrayList<>();

	@Override
	public void earlyStartup() {
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			activePage.setValue(window.getActivePage());
			window.addPageListener(new IPageListener() {
				@Override
				public void pageOpened(IWorkbenchPage page) {
					//Don't care
				}
				
				@Override
				public void pageClosed(IWorkbenchPage page) {
					//Don't care
				}
				
				@Override
				public void pageActivated(IWorkbenchPage page) {
					activePage.setValue(page);
				}
			});
		});
		
		DelegatingLiveExp<IWorkbenchPart> activePart = activePage.then((page) -> {
			if (page!=null) {
				LiveExpression<IWorkbenchPart> activePartExp = new LiveExpression<IWorkbenchPart>(page.getActivePart()) {
					@Override
					protected IWorkbenchPart compute() {
						return page.getActivePart();
					}
				};
				IPartListener pl = new IPartListener() {
					@Override
					public void partOpened(IWorkbenchPart part) {
					}
					
					@Override
					public void partDeactivated(IWorkbenchPart part) {
					}
					
					@Override
					public void partClosed(IWorkbenchPart part) {
					}
					
					@Override
					public void partBroughtToTop(IWorkbenchPart part) {
					}
					
					@Override
					public void partActivated(IWorkbenchPart part) {
						activePartExp.refresh();
					}
				};
				page.addPartListener(pl);
				activePartExp.onDispose(d -> page.removePartListener(pl));
				return activePartExp;
			}
			return null;
		});
		AutoDispose.installOn(activePart.getDelegate().unsafeCast(Disposable.class));
		activePart.onChange((e, ap) -> System.out.println("active part = "+ap));
		
		
		LiveExpression<CompilationUnitEditor> activeJavaEditor = activePart.filter(org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor.class);
		activeJavaEditor.onChange((e, editor) -> {
			if (editor!=null) {
				ISourceViewer sourceViewer = editor.getViewer();
				if (sourceViewer!=null) {
					IAnnotationModel annotationModel = sourceViewer.getAnnotationModel();
					if (annotationModel!=null) {
						IDocument doc = sourceViewer.getDocument();
						if (sourceViewer!=null) {
							if (doc!=null) {
								updateAnnotations(doc, annotationModel);
							}
						}
					}
				}
			}
		});
	}

	private synchronized void updateAnnotations(IDocument document, IAnnotationModel annotationModel) {
		for (Disposable disposable : disposables) {
			disposable.dispose();
		}
		disposables.clear();
		
		int start = document.get().indexOf(MARK_WORD);
		if (start>=0) {
			Annotation annotation = new Annotation(ANNOTION_TYPE, false, null);
			Position position = new Position(start, MARK_WORD.length());
			annotationModel.addAnnotation(annotation, position);
			disposables.add(() -> {
				annotationModel.removeAnnotation(annotation);
			});
		}
	}

}
