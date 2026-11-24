import hashlib
import math
import re
from langchain_core.embeddings import Embeddings

class LocalHashEmbeddings(Embeddings):
    """无需下载模型的关键词向量，方便离线跑通流程；不是语义大模型。"""
    dimensions = 384

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return [self.embed_query(text) for text in texts]

    def embed_query(self, text: str) -> list[float]:
        text = text.casefold()
        tokens = re.findall(r"[a-z0-9_]+|[\u4e00-\u9fff]", text)
        # 加入中文相邻字组合，使“事务”“回滚”等词的匹配比单字更明确。
        for run in re.findall(r"[\u4e00-\u9fff]+", text):
            tokens.extend(run[i:i+2] for i in range(len(run)-1))
        vector = [0.0] * self.dimensions
        for token in tokens:
            # Python 自带 hash() 跨进程不稳定；持久化向量必须使用稳定哈希。
            digest = hashlib.sha256(token.encode("utf-8")).digest()
            vector[int.from_bytes(digest[:4], "little") % self.dimensions] += 1.0
        norm = math.sqrt(sum(value * value for value in vector))
        if not norm:
            vector[0] = 1.0
            return vector
        return [value / norm for value in vector]
