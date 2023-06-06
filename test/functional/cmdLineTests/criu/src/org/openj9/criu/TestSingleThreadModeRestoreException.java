/*******************************************************************************
 * Copyright IBM Corp. and others 2022
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 * or the Apache License, Version 2.0 which accompanies this distribution and
 * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * This Source Code may also be made available under the following
 * Secondary Licenses when the conditions for such availability set
 * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 * General Public License, version 2 with the GNU Classpath
 * Exception [1] and GNU General Public License, version 2 with the
 * OpenJDK Assembly Exception [2].
 *
 * [1] https://www.gnu.org/software/classpath/license.html
 * [2] https://openjdk.org/legal/assembly-exception.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0-only WITH Classpath-exception-2.0 OR GPL-2.0-only WITH OpenJDK-assembly-exception-1.0
 *******************************************************************************/
package org.openj9.criu;

import org.eclipse.openj9.criu.CRIUSupport;
import org.eclipse.openj9.criu.JVMRestoreException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TestSingleThreadModeRestoreException {

	private static final Lock jucLock = new ReentrantLock();
	private static final Object synLock = new Object();

	public static void main(String[] args) {
		new TestSingleThreadModeRestoreException().testAll();
	}

	void testAll() {
		testSingleThreadModeRestoreExceptionSynLock();
		testSingleThreadModeRestoreExceptionJUCLock();
	}

	Thread doCheckpointJUCLock(CRIUSupport criu) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				boolean result = false;
				criu.registerPostRestoreHook(new Runnable() {
					public void run() {
						CRIUTestUtils.showThreadCurrentTime("PreCheckpointHook() before ReentrantLock");
						jucLock.lock();
						CRIUTestUtils.showThreadCurrentTime("PreCheckpointHook() within ReentrantLock");
						jucLock.unlock();
						CRIUTestUtils.showThreadCurrentTime("PreCheckpointHook() after ReentrantLock");
					}
				});

				try {
					System.out.println("Pre-checkpoint JUC LOCK");
					CRIUTestUtils.checkPointJVMNoSetup(criu, CRIUTestUtils.imagePath, false);
				} catch (JVMRestoreException jvmre) {
					result = true;
				}
				if (result) {
					System.out.println("testSingleThreadModeRestoreExceptionJUCLock: PASSED.");
				} else {
					System.out.println("testSingleThreadModeRestoreExceptionJUCLock: FAILED.");
				}
			}
		});
		return t;
	}

	void testSingleThreadModeRestoreExceptionJUCLock() {
		CRIUTestUtils
				.showThreadCurrentTime("testSingleThreadModeRestoreExceptionJUCLock() before ReentrantLock.lock()");
		jucLock.lock();
		CRIUSupport criu = CRIUTestUtils.prepareCheckPointJVM(CRIUTestUtils.imagePath);
		if (criu == null) {
			// "CRIU is not enabled" is to appear and cause the test failure.
			return;
		}

		try {
			// ensure the lock already taken before performing a checkpoint
			CRIUTestUtils.showThreadCurrentTime(
					"testSingleThreadModeRestoreExceptionJUCLock() before doCheckpointJUCLock()");
			Thread tpc = doCheckpointJUCLock(criu);
			tpc.start();
			// set timeout 10s
			tpc.join(10000);
			CRIUTestUtils.showThreadCurrentTime("testSingleThreadModeRestoreExceptionJUCLock() after doCheckpointJUCLock()");
		} catch (InterruptedException e) {
		}
		jucLock.unlock();
		CRIUTestUtils.showThreadCurrentTime(
				"testSingleThreadModeRestoreExceptionJUCLock() after after ReentrantLock.unlock()");

	}

	Thread doCheckpointSynLock(CRIUSupport criu) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				boolean result = false;
				criu.registerPostRestoreHook(new Runnable() {
					public void run() {
						CRIUTestUtils.showThreadCurrentTime("PreCheckpointHook() before synchronized on " + synLock);
						synchronized (synLock) {
							CRIUTestUtils.showThreadCurrentTime("PreCheckpointHook() within synchronized on " + synLock);
						}
						CRIUTestUtils.showThreadCurrentTime("PreCheckpointHook() after synchronized on " + synLock);
					}
				});

				try {
					System.out.println("Pre-checkpoint synchronization");
					CRIUTestUtils.checkPointJVMNoSetup(criu, CRIUTestUtils.imagePath, false);
				} catch (JVMRestoreException jvmre) {
					result = true;
				}
				if (result) {
					System.out.println("testSingleThreadModeRestoreExceptionSynLock: PASSED.");
				} else {
					System.out.println("testSingleThreadModeRestoreExceptionSynLock: FAILED.");
				}
			}
		});
		return t;
	}

	void testSingleThreadModeRestoreExceptionSynLock() {
		CRIUTestUtils.showThreadCurrentTime(
				"testSingleThreadModeRestoreExceptionSynLock() before synchronized on " + synLock);
		synchronized (synLock) {
			CRIUSupport criu = CRIUTestUtils.prepareCheckPointJVM(CRIUTestUtils.imagePath);
			if (criu == null) {
				// "CRIU is not enabled" is to appear and cause the test failure.
				return;
			}

			try {
				// ensure the lock already taken before performing a checkpoint
				CRIUTestUtils.showThreadCurrentTime(
						"testSingleThreadModeRestoreExceptionSynLock() before doCheckpointSynLock()");
				Thread tpc = doCheckpointSynLock(criu);
				tpc.start();
				// set timeout 10s
				tpc.join(10000);
				CRIUTestUtils.showThreadCurrentTime(
						"testSingleThreadModeRestoreExceptionSynLock() after doCheckpointSynLock()");
			} catch (InterruptedException e) {
			}
		}
		CRIUTestUtils.showThreadCurrentTime(
				"testSingleThreadModeRestoreExceptionSynLock() after synchronized on " + synLock);
	}
}
