package me.tatarka.androidunittest.idea;

import com.android.builder.model.JavaArtifact;
import com.android.builder.model.SourceProvider;
import com.android.tools.idea.gradle.customizer.ModuleCustomizer;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.JUnitConfigurationType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import me.tatarka.androidunittest.idea.util.DefaultManifestParser;
import me.tatarka.androidunittest.idea.util.ManifestParser;
import me.tatarka.androidunittest.model.AndroidUnitTest;
import me.tatarka.androidunittest.model.Variant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by evan on 6/7/14.
 */
public class RunConfigurationModuleCustomizer implements ModuleCustomizer<IdeaAndroidUnitTest> {
    @Override
    public void customizeModule(@NotNull Module module, @NotNull Project project, @Nullable IdeaAndroidUnitTest androidUnitTest) {
        if (androidUnitTest == null) return;
        JavaArtifact selectedTestJavaArtifact = androidUnitTest.getSelectedTestJavaArtifact();
        com.android.builder.model.Variant androidVariant = androidUnitTest.getSelectedAndroidVariant();

        if (selectedTestJavaArtifact != null) {
            String RPackageName = findRPackageName(androidUnitTest);

            String vmParameters = buildVmParameters(module, RPackageName, selectedTestJavaArtifact, androidVariant);

            RunManager runManager = RunManager.getInstance(project);
            runManager.getConfigurationFactories();
            JUnitConfigurationType junitConfigType = ConfigurationTypeUtil.findConfigurationType(JUnitConfigurationType.class);
            List<RunConfiguration> configs = runManager.getConfigurationsList(junitConfigType);

            for (RunConfiguration config : configs) {
                if (isRelevantRunConfig(module, config)) {
                    JUnitConfiguration jconfig = (JUnitConfiguration) config;
                    jconfig.setVMParameters(vmParameters);
                }
            }


            for (ConfigurationFactory factory : junitConfigType.getConfigurationFactories()) {
                RunnerAndConfigurationSettings settings = runManager.getConfigurationTemplate(factory);
                RunConfiguration config = settings.getConfiguration();
                if (isRelevantRunConfig(module, config)) {
                    JUnitConfiguration jconfig = (JUnitConfiguration) config;
                    jconfig.setVMParameters(vmParameters);
                }
            }
        } else {
            oldCustomizeModule(module, project, androidUnitTest);
        }
    }

    @Deprecated
    protected void oldCustomizeModule(@NotNull Module module, @NotNull Project project, @Nullable IdeaAndroidUnitTest androidUnitTest) {
        if (androidUnitTest == null) return;
        Variant selectedTestVariant = androidUnitTest.getSelectedTestVariant();
        if (selectedTestVariant != null) {
            AndroidUnitTest testDelegate = androidUnitTest.getTestDelegate();
            if (testDelegate == null) return;

            String RPackageName = testDelegate.getRPackageName();

            String vmParameters = oldBuildVmParameters(RPackageName, selectedTestVariant);

            RunManager runManager = RunManager.getInstance(project);
            runManager.getConfigurationFactories();
            JUnitConfigurationType junitConfigType = ConfigurationTypeUtil.findConfigurationType(JUnitConfigurationType.class);
            List<RunConfiguration> configs = runManager.getConfigurationsList(junitConfigType);


            for (RunConfiguration config : configs) {
                if (isRelevantRunConfig(module, config)) {
                    JUnitConfiguration jconfig = (JUnitConfiguration) config;
                    jconfig.setVMParameters(vmParameters);
                }
            }

            for (ConfigurationFactory factory : junitConfigType.getConfigurationFactories()) {
                RunnerAndConfigurationSettings settings = runManager.getConfigurationTemplate(factory);
                RunConfiguration config = settings.getConfiguration();
                if (isRelevantRunConfig(module, config)) {
                    JUnitConfiguration jconfig = (JUnitConfiguration) config;
                    jconfig.setVMParameters(vmParameters);
                }
            }
        }
    }

    private static String findRPackageName(IdeaAndroidUnitTest androidUnitTest) {
        String packageName = androidUnitTest.getAndroidDelegate().getDefaultConfig().getProductFlavor().getApplicationId();
        if (packageName == null) {
            File manifestFile = androidUnitTest.getAndroidDelegate().getDefaultConfig().getSourceProvider().getManifestFile();
            ManifestParser parser = new DefaultManifestParser();
            packageName = parser.getPackage(manifestFile);
        }
        return packageName;
    }

    private static boolean isRelevantRunConfig(Module module, RunConfiguration config) {
        if (!(config instanceof JUnitConfiguration)) return false;
        for (Module m : ((JUnitConfiguration) config).getModules()) {
            if (m == module) return true;
        }
        return false;
    }

    private static String buildVmParameters(Module module, String RPackageName, JavaArtifact testJavaArtifact, com.android.builder.model.Variant androidVariant) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> prop : getRobolectricProperties(RPackageName, testJavaArtifact).entrySet()) {
            builder.append("-D").append(prop.getKey()).append("=\"").append(prop.getValue()).append("\" ");
        }
        return builder.toString();
    }

    @Deprecated
    private static String oldBuildVmParameters(String RPackageName, Variant testVariant) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> prop : oldGetRobolectricProperties(RPackageName, testVariant).entrySet()) {
            builder.append("-D").append(prop.getKey()).append("=\"").append(prop.getValue()).append("\" ");
        }
        return builder.toString();
    }

    private static Map<String, String> getRobolectricProperties(String RPackageName, JavaArtifact testJavaArtifact) {
        SourceProvider sourceProvider = testJavaArtifact.getVariantSourceProvider();
        String manifestFile = sourceProvider.getManifestFile().getAbsolutePath();
        String resourcesDirs = fileCollectionToPath(sourceProvider.getResDirectories());
        String assetsDir = fileCollectionToPath(sourceProvider.getAssetsDirectories());

        Map<String, String> props = Maps.newHashMap();
        props.put("android.manifest", manifestFile);
        props.put("android.resources", resourcesDirs);
        props.put("android.assets", assetsDir);
        props.put("android.package", RPackageName);
        return props;
    }

    @Deprecated
    private static Map<String, String> oldGetRobolectricProperties(String RPackageName, Variant testVariant) {
        String manifestFile = testVariant.getManifest().getAbsolutePath();
        String resourcesDirs = testVariant.getResourcesDirectory().getAbsolutePath();
        String assetsDir = testVariant.getAssetsDirectory().getAbsolutePath();

        Map<String, String> props = Maps.newHashMap();
        props.put("android.manifest", manifestFile);
        props.put("android.resources", resourcesDirs);
        props.put("android.assets", assetsDir);
        props.put("android.package", RPackageName);
        return props;
    }

    private static String fileCollectionToPath(Collection<File> files) {
        return Joiner.on(File.pathSeparatorChar).join(Collections2.transform(files, new Function<File, String>() {
            @javax.annotation.Nullable
            @Override
            public String apply(@Nullable File file) {
                return file.getAbsolutePath();
            }
        }));
    }
}