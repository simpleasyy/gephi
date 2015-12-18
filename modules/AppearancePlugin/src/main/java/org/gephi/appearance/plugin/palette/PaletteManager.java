/*
 Copyright 2008-2013 Gephi
 Authors : Mathieu Bastian <mathieu.bastian@gephi.org>
 Website : http://www.gephi.org

 This file is part of Gephi.

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright 2013 Gephi Consortium. All rights reserved.

 The contents of this file are subject to the terms of either the GNU
 General Public License Version 3 only ("GPL") or the Common
 Development and Distribution License("CDDL") (collectively, the
 "License"). You may not use this file except in compliance with the
 License. You can obtain a copy of the License at
 http://gephi.org/about/legal/license-notice/
 or /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
 specific language governing permissions and limitations under the
 License.  When distributing the software, include this License Header
 Notice in each file and include the License files at
 /cddl-1.0.txt and /gpl-3.0.txt. If applicable, add the following below the
 License Header, with the fields enclosed by brackets [] replaced by
 your own identifying information:
 "Portions Copyrighted [year] [name of copyright owner]"

 If you wish your version of this file to be governed by only the CDDL
 or only the GPL Version 3, indicate your decision by adding
 "[Contributor] elects to include this software in this distribution
 under the [CDDL or GPL Version 3] license." If you do not indicate a
 single choice of license, a recipient has the option to distribute
 your version of this file under either the CDDL, the GPL Version 3 or
 to extend the choice of license to its licensees as provided above.
 However, if you add GPL Version 3 code and therefore, elected the GPL
 Version 3 license, then the option applies only if the new code is
 made subject to such option by the copyright holder.

 Contributor(s):

 Portions Copyrighted 2013 Gephi Consortium.
 */
package org.gephi.appearance.plugin.palette;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import org.openide.util.Exceptions;

/**
 *
 * @author mbastian
 */
public class PaletteManager {

    private static PaletteManager instance;

    public synchronized static PaletteManager getInstance() {
        if (instance == null) {
            instance = new PaletteManager();
        }
        return instance;
    }
    private final static int RECENT_PALETTE_SIZE = 5;
    private final List<Preset> presets;
    private final Collection<Palette> defaultPalettes;
    private final LinkedList<Palette> recentPalette;
    private final Color DEFAULT_COLOR = Color.LIGHT_GRAY;

    private PaletteManager() {
        presets = loadPresets();
        defaultPalettes = loadDefaultPalettes();
        recentPalette = new LinkedList<Palette>();
    }

    public Palette randomPalette(int colorCount) {
        List<Color> colors = new ArrayList<Color>();

        Random random = new Random();
        float B = random.nextFloat() * 2 / 5f + 0.6f;
        float S = random.nextFloat() * 2 / 5f + 0.6f;

        for (int i = 1; i <= colorCount; i++) {
            float H = i / (float) colorCount;
            Color c = Color.getHSBColor(H, S, B);
            colors.add(c);
        }

        Collections.shuffle(colors);

        return new Palette(colors.toArray(new Color[0]));
    }

    public Palette generatePalette(int colorCount) {
        return generatePalette(colorCount, null);
    }

    public Palette generatePalette(int colorCount, Preset preset) {
        int quality = 50;
        if (colorCount > 50) {
            quality = 25;
        } else if (colorCount > 100) {
            quality = 10;
        } else if (colorCount > 200) {
            quality = 5;
        } else if (colorCount > 300) {
            quality = 2;
        }
        Color[] cls = PaletteGenerator.generatePalette(colorCount, quality, preset.toArray());
        return new Palette(cls);
    }

    public Collection<Preset> getPresets() {
        return presets;
    }

    public Collection<Palette> getDefaultPalette(int colorCount) {
        List<Palette> palettes = new ArrayList<Palette>();
        int max = Integer.MIN_VALUE;
        for (Palette p : defaultPalettes) {
            if (p.size() == colorCount) {
                palettes.add(p);
            }
            max = Math.max(p.size(), max);
        }
        if (palettes.isEmpty()) {
            for (Palette p : defaultPalettes) {
                if (p.size() == max) {
                    Color[] cols = Arrays.copyOf(p.getColors(), colorCount);
                    for (int i = p.size(); i < cols.length; i++) {
                        cols[i] = DEFAULT_COLOR;
                    }
                    palettes.add(new Palette(cols));
                }
            }
        }
        return palettes;
    }

    public void addRecentPalette(Palette palette) {
        recentPalette.remove(palette);
        if (recentPalette.size() == RECENT_PALETTE_SIZE) {
            recentPalette.removeLast();
        }
        recentPalette.addFirst(palette);
    }

    public Collection<Palette> getRecentPalettes() {
        return recentPalette;
    }

    private List<Preset> loadPresets() {
        List<Preset> presetList = new ArrayList<Preset>();
        try {
            LineNumberReader reader = new LineNumberReader(new InputStreamReader(PaletteManager.class.getResourceAsStream("palette_presets.csv")));
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(",");
                //name,dark,hmin,hmax,cmin,cmax,lmin,lmax
                String name = split[0];
                boolean dark = Boolean.parseBoolean(split[1]);
                int hMin = Integer.parseInt(split[2]);
                int hMax = Integer.parseInt(split[3]);
                float cMin = Float.parseFloat(split[4]);
                float cMax = Float.parseFloat(split[5]);
                float lMin = Float.parseFloat(split[6]);
                float lMax = Float.parseFloat(split[7]);
                presetList.add(new Preset(name, dark, hMin, hMax, cMin, cMax, lMin, lMax));
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return presetList;
    }

    private static Collection<Palette> loadDefaultPalettes() {
        try {
            return loadPalettes("palette_default.csv");
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return Collections.EMPTY_LIST;
    }

    private static Collection<Palette> loadPalettes(String fileName) throws IOException {
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(PaletteManager.class.getResourceAsStream(fileName)));
        String line;
        List<List<Color>> palettes = new ArrayList<List<Color>>();
        while ((line = reader.readLine()) != null) {
            List<Color> palette = new ArrayList<Color>();
            String[] split = line.split(",");
            for (String colorStr : split) {
                if (!colorStr.isEmpty()) {
                    palette.add(parseHexColor(colorStr.trim()));
                }
            }
            if (!palette.isEmpty()) {
                palettes.add(palette);
            }
        }
        List<Palette> result = new ArrayList<Palette>();
        for (List<Color> cls : palettes) {
            Palette plt = new Palette(cls.toArray(new Color[0]));
            result.add(plt);
        }
        return result;
    }

    private static Color parseHexColor(String hexColor) {
        int rgb = Integer.parseInt(hexColor.replaceFirst("#", ""), 16);
        return new Color(rgb);
    }
}
