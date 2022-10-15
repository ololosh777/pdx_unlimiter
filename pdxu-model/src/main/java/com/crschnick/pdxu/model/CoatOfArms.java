package com.crschnick.pdxu.model;

import com.crschnick.pdxu.io.node.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CoatOfArms {

    private List<Sub> subs;

    public CoatOfArms() {
    }

    public CoatOfArms(List<Sub> subs) {
        this.subs = subs;
    }

    public static CoatOfArms empty() {
        return new CoatOfArms(List.of(Sub.empty()));
    }

    public static Map<Long, CoatOfArms> createCoaMap(Node node) {
        Map<Long, CoatOfArms> map = new HashMap<>();
        if (node == null) {
            return map;
        }

        node.forEach((k, v) -> {
            var coa = fromNode(v, null);
            map.put(Long.parseLong(k), coa);
        });
        return map;
    }

    public static CoatOfArms fromNode(Node n, Function<String, Node> parentResolver) {
        List<Sub> subs = new ArrayList<>();
        subs.addAll(Sub.fromNode(n, parentResolver));
        for (var subNode : n.getNodesForKey("sub")) {
            subs.addAll(Sub.fromNode(subNode, parentResolver));
        }
        return new CoatOfArms(subs);
    }

    public List<Sub> getSubs() {
        return subs;
    }

    public static final class Sub {

        private double x = 0.5;
        private double y = 0.5;
        private double scaleX = 1.0;
        private double scaleY = 1.0;
        private String patternFile;
        private List<String> colors;
        private List<Emblem> emblems;

        public Sub() {
        }

        public Sub(
                double x,
                double y,
                double scaleX,
                double scaleY,
                String patternFile,
                List<String> colors,
                List<Emblem> emblems
        ) {
            this.x = x;
            this.y = y;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.patternFile = patternFile;
            this.colors = colors;
            this.emblems = emblems;
        }

        static Sub empty() {
            return new Sub(0, 0, 1, 1, "pattern_solid.dds", List.of("black", "black"), List.of(Emblem.empty()));
        }

        public static List<Sub> fromNode(Node n, Function<String, Node> parentResolver) {
            // A sub entry must be an array
            if (!n.isArray()) {
                return List.of();
            }

            var parentNode = n.getNodeForKeyIfExistent("parent")
                    .filter(node -> parentResolver != null)
                    .map(node -> parentResolver.apply(node.getString()))
                    .orElse(null);

            List<Sub> subs = new ArrayList<>();
            if (!n.hasKey("instance")) {
                subs.add(subInstance(n, null, parentResolver));
            } else {
                for (var in : n.getNodesForKey("instance")) {
                    subs.add(subInstance(n, in, parentResolver));
                }
            }
            return subs;
        }

        public static Sub subInstance(Node n, Node instanceNode, Function<String, Node> parentResolver) {
            var parentNode = n.getNodeForKeyIfExistent("parent")
                    .filter(node -> parentResolver != null)
                    .map(node -> parentResolver.apply(node.getString()))
                    .orElse(null);
            var patternFile =
                    n.getNodeForKeyIfExistent("pattern").map(Node::getString).orElse(null);

            List<String> colors = new ArrayList<>();
            n.getNodeForKeyIfExistent("color1")
                    .filter(Node::isValue)
                    .map(Node::getString)
                    .ifPresent(colors::add);
            n.getNodeForKeyIfExistent("color2")
                    .filter(Node::isValue)
                    .map(Node::getString)
                    .ifPresent(colors::add);
            n.getNodeForKeyIfExistent("color3")
                    .filter(Node::isValue)
                    .map(Node::getString)
                    .ifPresent(colors::add);
            n.getNodeForKeyIfExistent("color4")
                    .filter(Node::isValue)
                    .map(Node::getString)
                    .ifPresent(colors::add);

            List<Emblem> emblems = new ArrayList<>();
            emblems.addAll(n.getNodesForKey("colored_emblem").stream()
                                   .map(Emblem::fromColoredEmblemNode)
                                   .collect(Collectors.toList()));

            emblems.addAll(n.getNodesForKey("textured_emblem").stream()
                                   .map(Emblem::fromTexturedEmblemNode)
                                   .collect(Collectors.toList()));

            double x = 0;
            double y = 0;
            if (instanceNode != null) {
                var offset = instanceNode.getNodeForKeyIfExistent("offset").orElse(null);
                if (offset != null) {
                    x = offset.getNodeArray().get(0).getDouble();
                    y = offset.getNodeArray().get(1).getDouble();
                }
            }

            double sx = 1;
            double sy = 1;
            if (instanceNode != null) {
                var scale = instanceNode.getNodeForKeyIfExistent("scale").orElse(null);
                if (scale != null) {
                    sx = scale.getNodeArray().get(0).getDouble();
                    sy = scale.getNodeArray().get(1).getDouble();
                }
            }

            return new Sub(x, y, sx, sy, patternFile, colors, emblems);
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getScaleX() {
            return scaleX;
        }

        public double getScaleY() {
            return scaleY;
        }

        public String getPatternFile() {
            return patternFile;
        }

        public List<String> getColors() {
            return colors;
        }

        public List<Emblem> getEmblems() {
            return emblems;
        }
    }

    public static final class Instance {
        private double x = 0.5;
        private double y = 0.5;

        private double scaleX = 1.0;
        private double scaleY = 1.0;

        private double rotation = 0;

        // Since 1.5 the depth can be a double
        private double depth = 0;

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getScaleX() {
            return scaleX;
        }

        public double getScaleY() {
            return scaleY;
        }

        public double getRotation() {
            return rotation;
        }

        public double getDepth() {
            return depth;
        }
    }

    public static class Emblem {

        private String file;
        private List<Integer> mask;
        private List<String> colors;
        private List<Instance> instances;

        private static Emblem empty() {
            Emblem c = new Emblem();
            c.file = "_default.dds";
            c.mask = new ArrayList<>();
            c.colors = List.of();
            c.instances = List.of(new Instance());
            return c;
        }

        private static Emblem fromTexturedEmblemNode(Node n) {
            Emblem c = new Emblem();
            n.getNodeForKeyIfExistent("texture")
                    .ifPresentOrElse(
                            tex -> {
                                c.file = tex.getString();
                            },
                            () -> c.file = "_default.dds"
                    );
            c.colors = new ArrayList<>();

            n.getNodeForKeyIfExistent("mask")
                    .ifPresentOrElse(
                            r -> {
                                c.mask = r.getNodeArray().stream()
                                        .map(Node::getInteger)
                                        .collect(Collectors.toList());
                            },
                            () -> c.mask = new ArrayList<>()
                    );

            c.instances = n.getNodesForKey("instance").stream()
                    .map(i -> {
                        Instance instance = new Instance();
                        i.getNodeForKeyIfExistent("position").ifPresent(p -> {
                            if (p.isArray()) {
                                instance.x = p.getNodeArray().get(0).getDouble();
                                instance.y = p.getNodeArray().get(1).getDouble();
                            }
                        });
                        i.getNodeForKeyIfExistent("scale").ifPresent(s -> {
                            if (s.isArray()) {
                                instance.scaleX = s.getNodeArray().get(0).getDouble();
                                instance.scaleY = s.getNodeArray().get(1).getDouble();
                            }
                        });
                        i.getNodeForKeyIfExistent("rotation").ifPresent(r -> {
                            instance.rotation = r.getDouble();
                        });
                        i.getNodeForKeyIfExistent("depth").ifPresent(r -> {
                            instance.depth = r.getDouble();
                        });
                        return instance;
                    })
                    .collect(Collectors.toList());

            if (c.instances.size() == 0) {
                c.instances.add(new Instance());
            }

            return c;
        }

        private static Emblem fromColoredEmblemNode(Node n) {
            Emblem c = new Emblem();
            n.getNodeForKeyIfExistent("texture")
                    .ifPresentOrElse(
                            tex -> {
                                c.file = tex.getString();
                            },
                            () -> c.file = "_default.dds"
                    );

            c.colors = new ArrayList<>();
            // Even color1 can sometimes be missing
            n.getNodeForKeyIfExistent("color1")
                    .filter(Node::isValue)
                    .map(Node::getString)
                    .ifPresent(c.colors::add);
            n.getNodeForKeyIfExistent("color2")
                    .filter(Node::isValue)
                    .map(Node::getString)
                    .ifPresent(c.colors::add);
            n.getNodeForKeyIfExistent("color3")
                    .filter(Node::isValue)
                    .map(Node::getString)
                    .ifPresent(c.colors::add);

            n.getNodeForKeyIfExistent("mask")
                    .ifPresentOrElse(
                            r -> {
                                c.mask = r.getNodeArray().stream()
                                        .map(Node::getInteger)
                                        .collect(Collectors.toList());
                            },
                            () -> c.mask = new ArrayList<>()
                    );

            c.instances = n.getNodesForKey("instance").stream()
                    .map(i -> {
                        Instance instance = new Instance();
                        i.getNodeForKeyIfExistent("position").ifPresent(p -> {
                            if (p.isArray()) {
                                instance.x = p.getNodeArray().get(0).getDouble();
                                instance.y = p.getNodeArray().get(1).getDouble();
                            }
                        });
                        i.getNodeForKeyIfExistent("scale").ifPresent(s -> {
                            if (s.isArray() && s.getArrayNode().size() == 1) {
                                instance.scaleX = s.getNodeArray().get(0).getDouble();
                                instance.scaleY = s.getNodeArray().get(0).getDouble();
                            } else if (s.isArray()) {
                                instance.scaleX = s.getNodeArray().get(0).getDouble();
                                instance.scaleY = s.getNodeArray().get(1).getDouble();
                            }
                        });
                        i.getNodeForKeyIfExistent("rotation").ifPresent(r -> {
                            instance.rotation = r.getDouble();
                        });
                        i.getNodeForKeyIfExistent("depth").ifPresent(r -> {
                            instance.depth = r.getDouble();
                        });
                        return instance;
                    })
                    .collect(Collectors.toList());
            if (c.instances.size() == 0) {
                c.instances.add(new Instance());
            }
            return c;
        }

        public String getFile() {
            return file;
        }

        public List<String> getColors() {
            return colors;
        }

        public List<Instance> getInstances() {
            return instances;
        }

        public List<Integer> getMask() {
            return mask;
        }
    }
}
