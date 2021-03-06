/****************************************************************************
 * Copyright 2016 Ruhr-Universität Bochum.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package de.rub.nds.oidc.browser;

import de.rub.nds.oidc.learn.TemplateEngine;
import de.rub.nds.oidc.log.TestStepLogger;
import de.rub.nds.oidc.test_model.ParameterType;
import de.rub.nds.oidc.test_model.TestOPConfigType;
import de.rub.nds.oidc.test_model.TestRPConfigType;
import de.rub.nds.oidc.test_model.TestStepResult;
import de.rub.nds.oidc.utils.Func;
import de.rub.nds.oidc.utils.InstanceParameters;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.velocity.context.Context;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.browserlaunchers.Sleeper;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 *
 * @author Tobias Wich
 */
public abstract class BrowserSimulator {

	protected RemoteWebDriver driver;

	protected long NORMAL_WAIT_TIMEOUT = 15;
	protected long SEARCH_WAIT_TIMEOUT = 1;

	protected TestRPConfigType rpConfig;
	protected TestOPConfigType opConfig;
	protected TemplateEngine te;

	protected TestStepLogger logger;
	protected Map<String, Object> suiteCtx;
	protected Map<String, Object> stepCtx;
	protected InstanceParameters params;

	public BrowserSimulator() {
		loadDriver(false);
	}

	protected final void loadDriver(boolean quit) {
		if (quit) {
			quit();
		}
		driver = new PhantomJSDriver();
		driver.manage().window().setSize(new Dimension(1024, 768));
		driver.manage().timeouts().implicitlyWait(NORMAL_WAIT_TIMEOUT, TimeUnit.SECONDS);
	}

	public void setRpConfig(TestRPConfigType rpConfig) {
		this.rpConfig = rpConfig;
	}

	public void setOpConfig(TestOPConfigType opConfig) {
		this.opConfig = opConfig;
	}

	public void setTemplateEngine(TemplateEngine te) {
		this.te = te;
	}

	public void setLogger(TestStepLogger logger) {
		this.logger = logger;
	}

	public void setContext(Map<String, Object> suiteCtx, Map<String, Object> stepCtx) {
		this.suiteCtx = suiteCtx;
		this.stepCtx = stepCtx;
	}

	public void setParameters(List<ParameterType> params) {
		this.params = new InstanceParameters(params);
	}

	protected Context createRPContext() {
		Context teCtx = te.createContext(rpConfig);
		enrichContext(teCtx);
		return teCtx;
	}

	protected Context createOPContext() {
		Context teCtx = te.createContext(opConfig);
		enrichContext(teCtx);
		return teCtx;
	}

	private void enrichContext(Context teCtx) {
		teCtx.put("suite", suiteCtx);
		teCtx.put("step", stepCtx);
		teCtx.put("params", params.getMap());
	}

	public abstract TestStepResult run();

	public void quit() {
		driver.quit();
	}

	protected final <T> T waitForPageLoad(Func<T> func) {
		RemoteWebElement oldHtml = (RemoteWebElement) driver.findElement(By.tagName("html"));

		T result = func.call();
		WebDriverWait wait = new WebDriverWait(driver, 30);
		wait.until((WebDriver input) -> {
			RemoteWebElement newHtml = (RemoteWebElement) driver.findElement(By.tagName("html"));
			return ! newHtml.getId().equals(oldHtml.getId());
		});

		return result;
	}

	protected void waitForDocumentReady() {
		WebDriverWait wait = new WebDriverWait(driver, 30);
		wait.until((WebDriver d) -> driver.executeScript("return document.readyState").equals("complete"));
	}

	protected void waitMillis(long timeout) {
		Sleeper.sleepTight(timeout);
	}

	protected void logScreenshot() {
		byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);
		logger.log(screenshot, "image/png");
	}

	protected <T> T withSearchTimeout(Func<T> fun) {
		try {
			driver.manage().timeouts().implicitlyWait(SEARCH_WAIT_TIMEOUT, TimeUnit.SECONDS);
			return fun.call();
		} finally {
			driver.manage().timeouts().implicitlyWait(NORMAL_WAIT_TIMEOUT, TimeUnit.SECONDS);
		}
	}

}
