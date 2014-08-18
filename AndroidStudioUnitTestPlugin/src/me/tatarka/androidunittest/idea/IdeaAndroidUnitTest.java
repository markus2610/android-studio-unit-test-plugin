package me.tatarka.androidunittest.idea;

import com.android.builder.model.AndroidProject;
import com.android.builder.model.JavaArtifact;
import com.android.builder.model.Variant;
import com.google.common.collect.Maps;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by evan on 6/5/14.
 */
public class IdeaAndroidUnitTest implements Serializable {
    private static WeakHashMap<AndroidProject, IdeaAndroidUnitTest> map = new WeakHashMap<AndroidProject, IdeaAndroidUnitTest>();

    @NotNull private final String myModuleName;
    @NotNull private final VirtualFile myRootDir;
    @NotNull private final AndroidProject myAndroidDelegate;
    @Nullable private String mySelectedVariantName;

    @NotNull private Map<String, Variant> myAndroidVariantsByName = Maps.newHashMap();
    @NotNull private Map<String, JavaArtifact> myTestJavaArtifacts = Maps.newHashMap();

    public IdeaAndroidUnitTest(@NotNull String moduleName, @NotNull File rootDir, @NotNull AndroidProject androidDelegate, @NotNull String selectedVariantName) {
        myModuleName = moduleName;
        VirtualFile found = VfsUtil.findFileByIoFile(rootDir, true);
        assert found != null;
        myRootDir = found;
        myAndroidDelegate = androidDelegate;

        populateVariantsByName();

        setSelectedVariantName(selectedVariantName);
        map.put(androidDelegate, this);
    }

    private void populateVariantsByName() {
        for (Variant variant : myAndroidDelegate.getVariants()) {
            myAndroidVariantsByName.put(variant.getName(), variant);

            for (JavaArtifact javaArtifact : variant.getExtraJavaArtifacts()) {
                if (javaArtifact.getName().equals("_unit_test_")) {
                    myTestJavaArtifacts.put(variant.getName(), javaArtifact);
                    break;
                }
            }
        }
    }

    /** * Updates the name of the selected build variant. If the given name does not belong to an existing variant, this method will pick up
     * the first variant, in alphabetical order.
     *
     * @param name the new name.
     */
    public void setSelectedVariantName(@NotNull String name) {
        Collection<String> variantNames = getVariantNames();
        String newVariantName = null;
        if (variantNames.contains(name)) {
            newVariantName = name;
        }
        mySelectedVariantName = newVariantName;
    }

    @Nullable
    public JavaArtifact getSelectedTestJavaArtifact() {
        return myTestJavaArtifacts.get(mySelectedVariantName);
    }

    @Nullable
    public Variant getSelectedAndroidVariant() {
        return myAndroidVariantsByName.get(mySelectedVariantName);
    }

    @NotNull
    public Collection<String> getVariantNames() {
        return myAndroidVariantsByName.keySet();
    }

    @NotNull
    public String getModuleName() { return myModuleName; }

    @NotNull
    public AndroidProject getAndroidDelegate() { return myAndroidDelegate; }

    @NotNull
    public VirtualFile getRootDir() { return myRootDir; }

    @Nullable
    public static IdeaAndroidUnitTest getFromAndroidProject(AndroidProject androidProject) {
        return map.get(androidProject);
    }
}
