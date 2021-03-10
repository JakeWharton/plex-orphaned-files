package com.jakewharton.plex

import com.google.common.truth.ThrowableSubject
import com.google.common.truth.Truth.assertThat

inline fun <reified T : Throwable> assertThrows(body: () -> Unit): ThrowableSubject {
	try {
		body()
	} catch (t: Throwable) {
		if (t is T) {
			return assertThat(t)
		}
		throw t
	}
	throw AssertionError(
		"Expected body to throw ${T::class.simpleName} but it completed successfully"
	)
}
