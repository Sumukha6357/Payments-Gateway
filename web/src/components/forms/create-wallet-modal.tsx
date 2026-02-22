"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/form-controls";
import { endpoints } from "@/lib/api/endpoints";
import { createIdempotencyKey } from "@/lib/utils/ids";

const schema = z.object({
  userId: z.string().uuid(),
  currency: z.string().length(3)
});

type FormValues = z.infer<typeof schema>;

export function CreateWalletModal() {
  const queryClient = useQueryClient();
  const [idempotencyKey] = useState(createIdempotencyKey());
  const { register, handleSubmit, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { currency: "USD" }
  });

  const mutation = useMutation({
    mutationFn: (values: FormValues) => endpoints.createWallet(values, idempotencyKey),
    onSuccess: () => {
      toast.success("Wallet created");
      queryClient.invalidateQueries({ queryKey: ["wallets"] });
    },
    onError: () => toast.error("Failed to create wallet")
  });

  return (
    <Dialog
      title="Create Wallet"
      trigger={<Button>Create Wallet</Button>}
      footer={<p className="text-xs text-slate-500">Idempotency-Key: {idempotencyKey}</p>}
    >
      <form className="space-y-3" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
        <Input placeholder="User UUID" {...register("userId")} />
        {errors.userId ? <p className="text-xs text-rose-600">{errors.userId.message}</p> : null}
        <Input placeholder="Currency" {...register("currency")} />
        <Button type="submit" className="w-full" disabled={mutation.isPending}>
          {mutation.isPending ? "Creating..." : "Create"}
        </Button>
      </form>
    </Dialog>
  );
}
