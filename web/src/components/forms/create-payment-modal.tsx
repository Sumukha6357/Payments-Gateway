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
  walletId: z.string().uuid(),
  amount: z.coerce.number().positive()
});

type FormValues = z.infer<typeof schema>;

export function CreatePaymentModal() {
  const queryClient = useQueryClient();
  const [idempotencyKey] = useState(createIdempotencyKey());
  const { register, handleSubmit } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const mutation = useMutation({
    mutationFn: (values: FormValues) => endpoints.createPayment(values, idempotencyKey),
    onSuccess: () => {
      toast.success("Payment created");
      queryClient.invalidateQueries({ queryKey: ["payments"] });
    },
    onError: () => toast.error("Payment failed")
  });

  return (
    <Dialog
      title="Create Payment"
      trigger={<Button>Create Payment</Button>}
      footer={<p className="text-xs text-slate-500">Idempotency-Key: {idempotencyKey}</p>}
    >
      <form className="space-y-3" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
        <Input placeholder="Wallet UUID" {...register("walletId")} />
        <Input type="number" step="0.01" placeholder="Amount" {...register("amount")} />
        <Button className="w-full" type="submit">Submit</Button>
      </form>
    </Dialog>
  );
}
