"""
Model conversion utility: PyTorch -> TensorFlow Lite (.tflite)

This script provides a scaffold for converting HuggingFace encoder models to
TFLite via an ONNX bridge. Fill in model/tokenizer specifics for your model.

Prereqs:
- Python 3.9+
- pip install torch transformers onnx onnxsim tensorflow==2.14.0 onnx-tf

Usage example:
  python tools/convert_to_tflite.py \
    --pytorch_model_dir ./minilm \
    --onnx_path ./build/minilm.onnx \
    --saved_model_dir ./build/minilm_savedmodel \
    --tflite_path ./build/minilm.tflite
"""

import argparse
import os
import sys


def parse_args():
    p = argparse.ArgumentParser()
    p.add_argument("--pytorch_model_dir", required=True)
    p.add_argument("--onnx_path", required=True)
    p.add_argument("--saved_model_dir", required=True)
    p.add_argument("--tflite_path", required=True)
    p.add_argument("--seq_len", type=int, default=128)
    return p.parse_args()


def export_to_onnx(hf_dir: str, onnx_path: str, seq_len: int) -> None:
    import torch
    from transformers import AutoModel

    model = AutoModel.from_pretrained(hf_dir)
    model.eval()

    input_ids = torch.randint(0, 30000, (1, seq_len), dtype=torch.long)
    attention_mask = torch.ones((1, seq_len), dtype=torch.long)

    inputs = (input_ids, attention_mask)

    torch.onnx.export(
        model,
        inputs,
        onnx_path,
        input_names=["input_ids", "attention_mask"],
        output_names=["last_hidden_state", "pooler_output"],
        dynamic_axes=None,
        opset_version=13,
    )


def simplify_onnx(onnx_path: str) -> str:
    from onnxsim import simplify
    import onnx

    model = onnx.load(onnx_path)
    model_simplified, check = simplify(model)
    if not check:
        print("[warn] ONNX simplify check failed; using original model")
        return onnx_path
    out_path = onnx_path.replace(".onnx", ".sim.onnx")
    onnx.save(model_simplified, out_path)
    return out_path


def onnx_to_savedmodel(onnx_path: str, saved_model_dir: str) -> None:
    from onnx_tf.backend import prepare
    import onnx
    import shutil

    if os.path.isdir(saved_model_dir):
        shutil.rmtree(saved_model_dir)
    os.makedirs(saved_model_dir, exist_ok=True)

    model = onnx.load(onnx_path)
    tf_rep = prepare(model)
    tf_rep.export_graph(saved_model_dir)


def savedmodel_to_tflite(saved_model_dir: str, tflite_path: str) -> None:
    import tensorflow as tf

    converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_dir)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()

    os.makedirs(os.path.dirname(tflite_path), exist_ok=True)
    with open(tflite_path, "wb") as f:
        f.write(tflite_model)
    print(f"[ok] Wrote {tflite_path}")


def main():
    args = parse_args()
    os.makedirs(os.path.dirname(args.onnx_path), exist_ok=True)

    print("[1/4] Exporting to ONNX...")
    export_to_onnx(args.pytorch_model_dir, args.onnx_path, args.seq_len)

    print("[2/4] Simplifying ONNX...")
    onnx_simplified = simplify_onnx(args.onnx_path)

    print("[3/4] Converting ONNX -> TF SavedModel...")
    onnx_to_savedmodel(onnx_simplified, args.saved_model_dir)

    print("[4/4] Converting SavedModel -> TFLite...")
    savedmodel_to_tflite(args.saved_model_dir, args.tflite_path)


if __name__ == "__main__":
    main()

