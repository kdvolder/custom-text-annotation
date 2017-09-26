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

import java.util.Objects;

import org.springsource.ide.eclipse.commons.livexp.core.LiveExpression;
import org.springsource.ide.eclipse.commons.livexp.ui.Disposable;

/**
 * AutoDispose utility. Tracks the value of a LiveExpression<Disposable> and dispose the
 * the old value in the LiveExp whenever it is changed.
 */
public class AutoDispose {

	private Disposable oldValue = null;
	
	private AutoDispose(LiveExpression<Disposable> target) {
		target.onChange(target, (e, newValue) -> {
			setValue(newValue);
		});
		target.onDispose(d -> setValue(null));
	}

	private synchronized void setValue(Disposable newValue) {
		if (!Objects.equals(oldValue, newValue)) {
			if (oldValue!=null) {
				oldValue.dispose();
			}
			oldValue = newValue;
		}
	}

	public static void installOn(LiveExpression<Disposable> target) {
		new AutoDispose(target);
	}

}
